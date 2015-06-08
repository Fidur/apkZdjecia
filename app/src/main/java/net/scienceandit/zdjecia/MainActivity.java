package net.scienceandit.zdjecia;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;

import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private Button btnLoadImages;
    private ProgressBar pbWheel;
    private ProgressBar pbHorizontal;

    private InstanceState state;
    private ImagesAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        state = (InstanceState) getLastNonConfigurationInstance();

        if (state == null)
            state = new InstanceState();

        GridView gvFriends = (GridView) findViewById(R.id.gvFriends);
        adapter = new ImagesAdapter(this, state.images);
        gvFriends.setAdapter(adapter);
        btnLoadImages = (Button) findViewById(R.id.btnLoadImages);
        pbHorizontal = (ProgressBar) findViewById(R.id.pbHorizontal);
        pbWheel = (ProgressBar) findViewById(R.id.pbWheel);

        if (state.task == null) {
            afterLoadingImages();
        } else {
            switch (state.task.getStatus()) {
                case PENDING:
                    state.task.attach(this);
                    state.task.execute();
                    break;
                case RUNNING:
                    state.task.attach(this);
                    setMaxProgress(state.task.getMaxProgress());
                    updateProgress(state.task.getProgress());
                    beforeLoadingImages();
                    break;
                case FINISHED:
                    afterLoadingImages();
                    break;
            }
        }
    }

    public Object onRetainNonConfigurationInstance() {
        if (state.task != null)
            state.task.detach();
        return state;
    }

    private void updateProgress(int progress) {
        pbHorizontal.setProgress(progress);
        adapter.notifyDataSetChanged();
    }

    private void setMaxProgress(int maxProgress) {
        pbHorizontal.setMax(maxProgress);
    }

    public void loadFriendImagesAsynchronously(View v) {
        state.images.clear();
        state.task = new ImageLoaderTask(this, state.images);
        state.task.execute();
    }

    private void beforeLoadingImages() {
        btnLoadImages.setEnabled(false);
        pbHorizontal.setVisibility(View.VISIBLE);
        pbWheel.setVisibility(View.VISIBLE);
    }

    private void afterLoadingImages() {
        btnLoadImages.setEnabled(true);
        pbHorizontal.setVisibility(View.GONE);
        pbWheel.setVisibility(View.INVISIBLE);

        state.task = null;
    }

    private static class InstanceState {
        ImageLoaderTask task;
        ArrayList<Bitmap> images = new ArrayList<Bitmap>();
    }

    private static class ImageLoaderTask extends AsyncTask<Void, Integer, ArrayList<Bitmap>> {
        private MainActivity activity;
        private Context context;
        private int progress;
        private int maxProgress;
        ArrayList<Bitmap> images;

        public ImageLoaderTask(MainActivity activity, ArrayList<Bitmap> images) {
            this.activity = activity;
            this.images = images;

            context = activity.getApplicationContext();
            progress = 0;
        }

        public void attach(MainActivity activity) {
            this.activity = activity;
        }

        public void detach() {
            activity = null;
        }

        public int getProgress() {
            return progress;
        }

        public int getMaxProgress() {
            return maxProgress;
        }

        private void setMaxProgress(int maxProgress) {
            this.maxProgress = maxProgress;

            if (activity != null) {
                activity.setMaxProgress(maxProgress);
            }
        }

        @Override
        protected void onPreExecute() {
            if (activity != null)
                activity.beforeLoadingImages();
        }

        @Override
        protected ArrayList<Bitmap> doInBackground(Void... params) {
            Cursor c = getAllContacts();
            if (c.moveToFirst()) {
                final int columnIndex = c.getColumnIndex(Contacts._ID);
                final int rowCount = c.getCount();

                setMaxProgress(rowCount);

                do {
                    Bitmap image = loadContactPhoto(c.getInt(columnIndex));
                    if (image != null)
                        images.add(image);
                    publishProgress(c.getPosition());
                } while (c.moveToNext());
            }
            c.close();
            return images;
        }

        private Cursor getAllContacts() {
            Uri uri = Contacts.CONTENT_URI;
            String[] projection = {Contacts._ID};
            return context.getContentResolver().query(uri, projection, Contacts.PHOTO_ID + " is not null", null, null);
        }

        private Bitmap loadContactPhoto(long id) {
            Uri uri = ContentUris.withAppendedId(Contacts.CONTENT_URI, id);
            InputStream input = Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);
            return BitmapFactory.decodeStream(input);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progress = values[0];

            if (activity != null)
                activity.updateProgress(progress);
        }

        @Override
        protected void onPostExecute(ArrayList<Bitmap> images) {
            if (activity != null)
                activity.afterLoadingImages();
        }
    }
}