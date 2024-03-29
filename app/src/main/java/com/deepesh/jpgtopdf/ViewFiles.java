package com.deepesh.jpgtopdf;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ViewFiles.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class ViewFiles extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private OnFragmentInteractionListener mListener;

    private static final int NAME_INDEX = 0;
    private static final int DATE_INDEX = 1;

    Activity activity;
    FilesAdapter adapter;
    ListView listView;
    SwipeRefreshLayout swipeView;
    public static TextView emptyStatusTextView;

    public ViewFiles() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        /*if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }*/
        activity=(Activity)context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_view_files, container, false);

        listView = (ListView) root.findViewById(R.id.list);
        swipeView = (SwipeRefreshLayout) root.findViewById(R.id.swipe);
        emptyStatusTextView = (TextView) root.findViewById(R.id.emptyStatusTextView);

        //Create/Open folder
        File folder = getOrCreatePdfDirectory();

        // Initialize variables
        final ArrayList<File> pdfFiles = new ArrayList<>();
        final File[] files = folder.listFiles();
        if (files.length == 0) {
            emptyStatusTextView.setVisibility(View.VISIBLE);
        }
        adapter = new FilesAdapter(activity, pdfFiles);
        listView.setAdapter(adapter);
        swipeView.setOnRefreshListener(this);

        // Populate data into listView
        populatePdfList();

        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_view_files_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_sort:
                displaySortDialog();
                break;
            default:
                break;
        }
        return true;
    }


    @Override
    public void onRefresh() {
        Log.v("refresh", "refreshing dta");
        populatePdfList();
        swipeView.setRefreshing(false);
    }

    private void populatePdfList() {
        new PopulateList().execute();
    }

    private class PopulateList extends AsyncTask<Void, Void,Void> {

        // Progress dialog
        MaterialDialog.Builder builder = new MaterialDialog.Builder(activity)
                .title(getActivity().getResources().getString(R.string.please_wait))
                .content(getActivity().getResources().getString(R.string.populating_list))
                .cancelable(false)
                .progress(true, 0);
        MaterialDialog dialog = builder.build();

        @Override
        protected Void doInBackground(Void... voids) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    populateListView();
                }
            });
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            dialog.dismiss();
        }

        /**
         * Populate data into listView
         */
        private void populateListView() {
            ArrayList<File> pdfFiles = new ArrayList<>();
            final File[] files = getOrCreatePdfDirectory().listFiles();
            if (files == null)
                Toast.makeText(activity, R.string.toast_no_pdfs, Toast.LENGTH_LONG).show();
            else {
                pdfFiles = getPdfsFromPdfFolder();
            }
            Log.v("done", "adding");
            adapter.setData(pdfFiles);
            listView.setAdapter(adapter);
        }
    }


    private void displaySortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Sort by")
                .setItems(R.array.sort_options, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ArrayList<File> pdfsFromFolder = getPdfsFromPdfFolder();
                        switch (which) {
                            case DATE_INDEX:
                                sortFilesByDateNewestToOldest(pdfsFromFolder);
                                adapter.setData(pdfsFromFolder);
                                break;
                            case NAME_INDEX:
                                sortByNameAlphabetical(pdfsFromFolder);
                                adapter.setData(pdfsFromFolder);
                                break;
                            default:
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    private void sortByNameAlphabetical(ArrayList<File> pdfsFromFolder) {
        Collections.sort(pdfsFromFolder);
    }

    private void sortFilesByDateNewestToOldest(ArrayList<File> pdfsFromFolder) {
        Collections.sort(pdfsFromFolder, new Comparator<File>() {
            @Override
            public int compare(File file, File file2) {
                return Long.compare(file2.lastModified(), file.lastModified());
            }
        });
    }

    private ArrayList<File> getPdfsFromPdfFolder() {
        return getPdfsFromFolder(getOrCreatePdfDirectory().listFiles());
    }

    private File getOrCreatePdfDirectory() {
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + getString(R.string.pdf_dir));
        if (!folder.exists()) {
            folder.mkdir();
        }
        return folder;
    }

    private ArrayList<File> getPdfsFromFolder(File[] files) {
        final ArrayList<File> pdfFiles = new ArrayList<>();
        for (File file : files) {
            if (!file.isDirectory() && file.getName().endsWith(getString(R.string.pdf_ext))) {
                pdfFiles.add(file);
                Log.v("adding", file.getName());
            }
        }
        return pdfFiles;
    }



    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
