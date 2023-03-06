package com.example.snaplapse.image_details

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.snaplapse.R
import com.example.snaplapse.api.RetrofitHelper
import com.example.snaplapse.api.data.photo.PhotoActionRequest
import com.example.snaplapse.api.routes.LocationsApi
import com.example.snaplapse.api.routes.PhotosApi
import com.example.snaplapse.api.routes.UsersApi
import com.example.snaplapse.timeline.TimelineFragment
import com.example.snaplapse.view_models.ItemsViewModel
import com.example.snaplapse.view_models.ItemsViewModel2

class ImageDetailsFragment(var item: ItemsViewModel, private val mList: List<ItemsViewModel>, val item2: ItemsViewModel2? = null) : Fragment() {

    private var likeButton: ImageButton? = null
    private var flagButton: ImageButton? = null

    private val usersApi = RetrofitHelper.getInstance().create(UsersApi::class.java)
    private val locationsApi = RetrofitHelper.getInstance().create(LocationsApi::class.java)
    private val photosApi = RetrofitHelper.getInstance().create(PhotosApi::class.java)

    private var userID: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_image_details, container, false)
        val text = view.findViewById<TextView>(R.id.textView)
        val img: ImageView = view.findViewById(R.id.imageView) as ImageView
        val userName = view.findViewById<TextView>(R.id.userName)
        val location = view.findViewById<TextView>(R.id.location)
        val date = view.findViewById<TextView>(R.id.date)
        val likeCount = view.findViewById<TextView>(R.id.likeCount)

        val photoId: Int
        val description: String

        userID = activity?.getSharedPreferences(getString(R.string.preferences_file_key), Context.MODE_PRIVATE)?.getInt("id", 0)!!

        if (item2 == null) {
            photoId = item.id
            description = item.text
            img.setImageResource(item.image)
            img.setOnTouchListener(OnSwipeTouchListener(activity, item, mList, view))
        } else {
            photoId = item2.id
            description = item2.text
            img.setImageBitmap(item2.image)
        }
        text.text = description
        date.text = item2!!.date

        lifecycleScope.launchWhenCreated {
            try {
                val userResponse = usersApi.getUser(userID)
                if (userResponse.isSuccessful) {
                    userName.text = userResponse.body()!!.username
                }

                val photoResponse = photosApi.getPhoto(photoId)
                if (photoResponse.isSuccessful) {
                    val locationId = photoResponse.body()!!.location
                    val locationResponse = locationsApi.getLocation(locationId)
                    if (locationResponse.isSuccessful) {
                        location.text = locationResponse.body()!!.name
                        location.setOnClickListener {
                            val timelineFragment = TimelineFragment(locationId)
                            val fragmentTransaction = parentFragmentManager.beginTransaction()
                            fragmentTransaction.replace(R.id.fragmentContainerView, timelineFragment)
                            fragmentTransaction.addToBackStack(null)
                            fragmentTransaction.commit()
                        }
                    }
                }

                val likeResponse = photosApi.getLike(null, photoId)
                if (likeResponse.isSuccessful) {
                    likeCount.text = likeResponse.body()!!.results.size.toString()
                }
            } catch (e: Exception) {
                Log.e("GetPhotoError", e.toString())
            }
        }

        likeButton = view.findViewById(R.id.like_button)
        likeButton!!.setOnClickListener {
            likePhoto(userID, likeCount)
        }
        getLike(userID)

        flagButton = view.findViewById(R.id.flag_button)
        flagButton!!.setOnClickListener {
            flagImage(userID)
        }
        getFlag(userID)

        val backButton = view.findViewById<ImageButton>(R.id.image_details_back_button)
        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        return view
    }

    private fun getLike(id: Int) {
        lifecycleScope.launchWhenCreated {
            try {
                val getLikeResponse = photosApi.getLike(user=id, photo=item2?.id?:0)
                if (getLikeResponse.isSuccessful) {
                    if (getLikeResponse.body()!!.results.isNotEmpty()) {
                        likeButton?.setImageResource(R.drawable.ic_baseline_thumb_up_24_blue)
                    }
                }
            } catch (e: Exception) {
                Log.e("getLikeError", e.toString())
            }
        }
    }

    private fun getFlag(id: Int) {
        lifecycleScope.launchWhenCreated {
            try {
                val getFlagResponse = photosApi.getFlag(user=id, photo=item2?.id?:0)
                if (getFlagResponse.isSuccessful) {
                    if (getFlagResponse.body()!!.results.isNotEmpty()) {
                        flagButton?.setImageResource(R.drawable.ic_baseline_flag_24_red)
                    }
                }
            } catch (e: Exception) {
                Log.e("getFlagError", e.toString())
            }
        }
    }

    private fun likePhoto(id: Int, likeCount: TextView) {
        lifecycleScope.launchWhenCreated {
            try {
                val getLikeResponse = photosApi.getLike(user=id, photo=item2?.id?:0)
                if (getLikeResponse.isSuccessful) {
                    if (getLikeResponse.body()!!.results.isEmpty()) {
                        val requestBody = PhotoActionRequest(user=id, photo=item2?.id?:0)
                        val response = photosApi.like(requestBody)
                        if (response.isSuccessful) {
                            likeButton?.setImageResource(R.drawable.ic_baseline_thumb_up_24_blue)
                            likeCount.text = (likeCount.text.toString().toInt() + 1).toString()
                        }
                        else {
                            Toast.makeText(activity, "Error Liking", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val response = photosApi.unlike(id=getLikeResponse.body()!!.results[0].id)
                        if (response.isSuccessful) {
                            likeButton?.setImageResource(R.drawable.ic_baseline_thumb_up_24)
                            likeCount.text = (likeCount.text.toString().toInt() - 1).toString()
                        }
                        else {
                            Toast.makeText(activity, "Error Unliking", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LikePhotoError", e.toString())
            }
        }
    }

    private fun flagImage(id: Int) {
        lifecycleScope.launchWhenCreated {
            try {
                val getFlagResponse = photosApi.getFlag(user=id, photo=item2?.id?:0)
                if (getFlagResponse.isSuccessful) {
                    if (getFlagResponse.body()!!.results.isEmpty()) {
                        val requestBody = PhotoActionRequest(user=id, photo=item2?.id?:0)
                        val response = photosApi.flag(requestBody)
                        if (response.isSuccessful) {
                            flagButton?.setImageResource(R.drawable.ic_baseline_flag_24_red)
                            Toast.makeText(activity, resources.getString(R.string.flag_toast), Toast.LENGTH_SHORT).show()
                        }
                        else {
                            Toast.makeText(activity, "Error Flagging", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val response = photosApi.unflag(id=getFlagResponse.body()!!.results[0].id)
                        if (response.isSuccessful) {
                            flagButton?.setImageResource(R.drawable.ic_baseline_flag_24)
                            Toast.makeText(activity, resources.getString(R.string.flag_toast), Toast.LENGTH_SHORT).show()
                        }
                        else {
                            Toast.makeText(activity, "Error Unflagging", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FlagPhotoError", e.toString())
            }
        }
//        FlagDialogFragment().show(childFragmentManager, "")
    }
}