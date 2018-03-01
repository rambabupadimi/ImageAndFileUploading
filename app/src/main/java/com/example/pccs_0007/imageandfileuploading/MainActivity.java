package com.example.pccs_0007.imageandfileuploading;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.interfaces.UploadProgressListener;
import com.bumptech.glide.Glide;
import com.rx2androidnetworking.Rx2AndroidNetworking;
import com.theartofdev.edmodo.cropper.CropImage;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    ImageView uploadImage;
    ProgressBar progressBar;
    Button upload;
    private AlertDialog alert;
    TextView progressText;
    Uri currentImageUri=null;
    String[] PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.CAMERA"};
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static int SELECT_FILE_CAMERA = 1;
    private static int SELECT_FILE_GALLERY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        upload      =   findViewById(R.id.upload_icon);
        progressBar =   findViewById(R.id.progress_bar);
        uploadImage =   findViewById(R.id.image_view);
        progressText    =   findViewById(R.id.progress_text);

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                marshmallowDialog();
            }
        });

    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == RESULT_OK) {

            if (requestCode == SELECT_FILE_GALLERY) {
                Log.i("tag","request code"+requestCode);
                Log.i("tag","request result code"+resultCode);
                Log.i("tag","request result code type1"+data.getData());
                if(data.getData()!=null)
                    performCrop(data.getData());

            } else if (requestCode == SELECT_FILE_CAMERA) {
                Log.i("tag","request code"+requestCode);
                Log.i("tag","request result code"+resultCode);
                Log.i("tag","request result code type1"+currentImageUri);
                if(currentImageUri!=null)
                    performCrop(currentImageUri);


            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {
                    Uri resultUri = result.getUri();
                    try {
                        Bitmap thePic = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                        String dateformat = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                        String filename =dateformat+".jpg";

                       // ByteArrayOutputStream stream = new ByteArrayOutputStream();
                       // thePic.compress(Bitmap.CompressFormat.JPEG, 95, stream);
                        String path =  SaveImageGallery(thePic, null, filename, null);
                        if(path!=null) {
                            Glide.with(this)
                                    .load(path)
                                    .placeholder(R.drawable.default_image)
                                    .error(R.drawable.default_image)
                                    .into(uploadImage);
                        }
                        /**
                         * http://rameshbookmeds-001-site1.ftempurl.com/api/Upload/MediaUpload
                         */

                        Rx2AndroidNetworking.upload("http://rameshbookmeds-001-site1.ftempurl.com/api/Upload/MediaUpload")
                                .addMultipartFile("image", new File(path))
                                .build()
                                .setUploadProgressListener(new UploadProgressListener() {
                                    @Override
                                    public void onProgress(long bytesUploaded, long totalBytes) {
                                        // do anything with progress
                                        double progress = (100.0 * bytesUploaded)/totalBytes;
                                        progressBar.setProgress((int)progress);


                                        double val1 = bytesUploaded/(1024*1024);
                                        double val2 = totalBytes/(1024*1024);

                                        String text = val1+" / "+val2+" mbs";
                                        progressText.setText(""+text);
                                    }
                                })
                                .getJSONObjectObservable()
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Observer<JSONObject>() {

                                    @Override
                                    public void onError(Throwable e) {
                                        // handle error
                                        Log.i("tag","throwable");
                                    }

                                    @Override
                                    public void onComplete() {
                                        Log.i("tag","complete");

                                    }

                                    @Override
                                    public void onSubscribe(Disposable d) {
                                        Log.i("tag","disposale");
                                    }

                                    @Override
                                    public void onNext(JSONObject response) {
                                        //do anything with response
                                        Log.i("tag","next"+response);
                                    }
                                });


                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                }

            }
        } else {
            Log.i("tag", "result code = -1");
        }

    }




    private void performCrop(Uri tempUri) {
        try {
            CropImage.activity(tempUri)
                    .setAspectRatio(1, 1)
                    .start(MainActivity.this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }






    private void getPhotoFromCamera()
    {
        try {

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            try {
                currentImageUri = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
            } else {
                File file = new File(currentImageUri.getPath());

                //Uri photoUri = Uri.fromFile(file);
                Uri photoUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

            }

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (intent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
                startActivityForResult(intent, SELECT_FILE_CAMERA);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void getPhotoFromGallery()
    {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select picture to upload "), SELECT_FILE_GALLERY);

    }


    private void marshmallowDialog( ) {
        try {
            if (Build.VERSION.SDK_INT >= 23 && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= 23 && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermission();
            } else {
                ShowOptions();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void requestPermission() {
        try {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                    ) {

                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
            } else {

                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }



    protected void ShowOptions() {

        final CharSequence[] items = {"Take New Photo", "Choose from Gallery",
                "Remove", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                Boolean isSDPresent = Environment
                        .getExternalStorageState().equals(
                                Environment.MEDIA_MOUNTED);
                if (item == 0) {
                    if (isSDPresent)
                        getPhotoFromCamera();
                    else
                        Toast.makeText(
                                MainActivity.this,
                                "Please turn off USB storage or insert your SD card and try again",
                                Toast.LENGTH_SHORT).show();

                    return;
                } else if (item == 1) {
                    if (isSDPresent)
                        getPhotoFromGallery();
                    else
                        Toast.makeText(
                                MainActivity.this,
                                "Please turn off USB storage or insert your SD card and try again",
                                Toast.LENGTH_SHORT).show();
                    return;

                } else if (item == 2) {

                            currentImageUri = null;
                            uploadImage.setImageBitmap(null);
                            uploadImage.setImageDrawable(getResources().getDrawable(R.drawable.default_image));

                    return;

                } else
                    alert.cancel();
            }

        });

        alert = builder.create();
        alert.show();
    }

    public static Uri createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date());
        File storageDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = null;
        if (storageDir.exists()) {
            image = File.createTempFile(timeStamp, /* prefix */
                    ".jpg", /* suffix */
                    storageDir /* directory */
            );
        } else {
            storageDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            if (storageDir.exists()) {
                image = File.createTempFile(timeStamp, /* prefix */
                        ".jpg", /* suffix */
                        storageDir /* directory */
                );
            }
        }
        // Save a file: path for use with ACTION_VIEW intents
        // mCurrentPhotoPath = image.getAbsolutePath();
        return Uri.fromFile(image);
    }




    public static String SaveImageGallery(Bitmap bitmap, byte[] byteArrayData, String filename, String imgActualPath) {
        File file = null, videoDirectory, AudioDirectory, galleryDirectory, directory, documentDirectory, profileDirectory;
        String root;
        Bitmap localBitMap;
        FileOutputStream fos;
        boolean isGallery = false, isOthers = false;
        try {
            root = Environment.getExternalStorageDirectory().toString();
            directory = new File(root + "/UploadImage");
            if (!directory.exists())
                directory.mkdirs();
            videoDirectory = directory.getParentFile();
            AudioDirectory = directory.getParentFile();
            galleryDirectory = directory.getParentFile();
            videoDirectory = new File(directory + "/UploadImage Videos");
            AudioDirectory = new File(directory + "/UploadImage Audio");
            galleryDirectory = new File(directory + "/UploadImage Images");
            documentDirectory = new File(directory + "/UploadImage Documents");

            if (!videoDirectory.exists())
                videoDirectory.mkdirs();
            if (!AudioDirectory.exists())
                AudioDirectory.mkdirs();
            if (!galleryDirectory.exists())
                galleryDirectory.mkdirs();
            if (!documentDirectory.exists())
                documentDirectory.mkdirs();

            if (filename != null) {
                if (filename.toString().endsWith(".jpg")
                        || filename.toString().endsWith(".png")
                        || filename.toString().endsWith(".jpeg")) {


                        file = new File(galleryDirectory, filename);

                    isGallery = true;

                } else if (filename.toString().endsWith(".pdf")
                        || filename.toString().endsWith(".doc")
                        || filename.toString().endsWith(".docx")
                        || filename.toString().endsWith(".ppt")
                        || filename.toString().endsWith(".pptx")
                        || filename.toString().endsWith(".xls")
                        || filename.toString().endsWith(".xlsx")
                        || filename.toString().endsWith(".txt")) {
                    file = new File(documentDirectory, filename);
                    isOthers = true;
                } else if (filename.toString().endsWith(".mp3")
                        || filename.toString().endsWith(".ogg")
                        || filename.toString().endsWith(".m4a")
                        || filename.toString().endsWith(".amr")
                        || filename.toString().endsWith(".3gpp")) {
                    file = new File(AudioDirectory, filename);
                    isOthers = true;
                } else if (filename.toString().endsWith(".mp4")
                        || filename.toString().endsWith(".3gp")
                        || filename.toString().endsWith(".avi")
                        || filename.toString().endsWith(".mkv")) {
                    file = new File(videoDirectory, filename);
                    isOthers = true;
                }
            }
            if (file != null && file.exists())
                file.delete();
            try {
                if (file != null) {
                    fos = new FileOutputStream(file, false);
                    if (isGallery) {
                        if (imgActualPath != null) {
                            localBitMap = BitmapFactory.decodeFile(imgActualPath);
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            localBitMap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            byteArrayData = stream.toByteArray();
                            fos.write(byteArrayData);
                        } else if (byteArrayData != null) {
                            // bitmap = BitmapFactory.decodeFile(filpath);
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                          //  bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            // getByteArray = stream.toByteArray();
                            fos = new FileOutputStream(file, false);
                            fos.write(byteArrayData);
                            fos.flush();
                            fos.close();
                        } else
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    } else if (isOthers) {
                        fos.write(byteArrayData);
                    }
                    fos.flush();
                    fos.close();
                    return galleryDirectory + "/" + filename;
                }

            } catch (Exception e) {
                e.printStackTrace();
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }
        return "";
    }
}
