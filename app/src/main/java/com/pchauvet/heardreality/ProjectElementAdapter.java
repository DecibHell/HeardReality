package com.pchauvet.heardreality;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

public class ProjectElementAdapter  extends ArrayAdapter<HeardProject> implements View.OnClickListener {

    private List<HeardProject> filteredProjects;
    private List<HeardProject> projects;
    private Filter projectFilter;


    private View view;

    private TextView projectName;
    private TextView projectOwner;
    private TextView projectUnpublished;
    private ImageView projectDownloaded;
    private ImageButton projectDetail;

    public ProjectElementAdapter(Context context) {
        super(context, R.layout.project_list_element, FirestoreManager.projects);
        this.projects = new ArrayList<>(FirestoreManager.projects);
        this.filteredProjects = new ArrayList<>(FirestoreManager.projects);
    }

    public void updateItems() {
        this.projects.clear();
        this.projects.addAll(FirestoreManager.projects);
        this.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        int position=(Integer) view.getTag();
        Object object= getItem(position);
        HeardProject project=(HeardProject)object;

        // BLABLA
    }

    @NonNull
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        // Get the data item for this position
        HeardProject project = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            this.view = inflater.inflate(R.layout.project_list_element, parent, false);
        } else {
            this.view = view;
        }

        projectName = this.view.findViewById(R.id.ple_name);
        projectName.setText(project.getName());

        projectOwner = this.view.findViewById(R.id.ple_owner);
        User owner = FirestoreManager.getUser(project.getOwner());
        projectOwner.setText(owner==null ? null : this.view.getResources().getString(R.string.wmf_by_owner, owner.getName()));

        projectUnpublished = this.view.findViewById(R.id.ple_unpublished);
        projectUnpublished.setVisibility(project.isPublished() ? View.GONE : View.VISIBLE);

        projectDownloaded = this.view.findViewById(R.id.ple_downloaded);
        projectDownloaded.setVisibility(StorageManager.downloadedProjects.contains(project.getId()) ? View.VISIBLE : View.GONE);

        this.view.setBackground(position%2 == 0 ? this.view.getResources().getDrawable(R.color.white, null) : this.view.getResources().getDrawable(R.color.lightGrey, null));

        // Return the completed view to render on screen
        return this.view;
    }

    @NonNull
    @Override
    public Filter getFilter()
    {
        if (projectFilter == null)
            projectFilter = new ProjectElementFilter();

        return projectFilter;
    }

    private class ProjectElementFilter extends Filter
    {
        @Override
        protected FilterResults performFiltering(CharSequence constraint)
        {
            FilterResults results = new FilterResults();
            String[] filters = constraint.toString().split("\\\\/");

            ArrayList<HeardProject> list = new ArrayList<>(projects);
            ArrayList<HeardProject> nlist = new ArrayList<>();

            for (int i=0; i<list.size(); i++)
            {
                HeardProject project = list.get(i);

                // Check the name filter
                if(project.getName().toLowerCase().contains(filters[0].toLowerCase())){
                    boolean isDownloaded = StorageManager.downloadedProjects.contains(project.getId());
                    boolean isOwned = AuthManager.currentUser!=null && AuthManager.currentUser.getUid().equals(project.getOwner());

                    // Check the column filter
                    if(filters[1].equals("ALL") || (filters[1].equals("DOWNLOADED") && isDownloaded) || (filters[1].equals("MYPROJECTS") && isOwned)){
                        nlist.add(project);
                    }
                }
            }
            results.values = nlist;
            results.count = nlist.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredProjects = (ArrayList<HeardProject>) results.values;
            clear();
            if(filteredProjects!=null){
                int count = filteredProjects.size();
                for (int i=0; i<count; i++)
                {
                    add(filteredProjects.get(i));
                }
            }
        }

    }
}