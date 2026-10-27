package com.developerfour.lastoption

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel

class splashscreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splashscreen)

        val imageList = ArrayList<SlideModel>() // Create image list

        // Adding images with ScaleType CENTER_CROP
        imageList.add(SlideModel(R.drawable.navimg1, ScaleTypes.CENTER_CROP))
        imageList.add(SlideModel(R.drawable.navimg2, ScaleTypes.CENTER_CROP))

        val imageSlider = findViewById<ImageSlider>(R.id.image_slider)
        imageSlider.setImageList(imageList)

        // Find the Next Button and Set Click Listener
        val nextBtn = findViewById<Button>(R.id.nextbtn)
        nextBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close the SplashScreen Activity
        }
    }
}
