package com.antwish.povi.familyconnect;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A fragment representing a single step in a wizard.
 *
 * <p>This class is used by the {@link TutorialActivity}</p>
 */
public class TutorialFragment extends Fragment {
    /**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "ARG_PAGE";

    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int mPage;

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given page number.
     */
    public static TutorialFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        TutorialFragment fragment = new TutorialFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public TutorialFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPage = getArguments().getInt(ARG_PAGE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout containing an imageView, textView and button
        View view = inflater.inflate(R.layout.fragment_tutorial, container, false);

        // Set images and texts according to the page number
        ImageView tutorialImage = (ImageView) view.findViewById(R.id.tutorialImageView);
        TextView tutorialTitle = (TextView) view.findViewById(R.id.boardingTitle);
        TextView tutorialCaption = (TextView) view.findViewById(R.id.boardingCaption);

        switch(mPage){
            case 0:
                // Set the imageView to show the respective image
                tutorialImage.setImageResource(R.drawable.onboard_1);

                // Set the textView to show the text
                tutorialTitle.setText(getResources().getString(R.string.tutorial_title_1));
                tutorialCaption.setText(getResources().getString(R.string.tutorial_caption_1));
                view.setBackgroundColor(getResources().getColor(R.color.red));
                break;
            case 1:
                // Set the imageView to show the respective image
                tutorialImage.setImageResource(R.drawable.onboard_2);

                // Set the textView to show the text
                tutorialTitle.setText(getResources().getString(R.string.tutorial_title_2));
                tutorialCaption.setText(getResources().getString(R.string.tutorial_caption_2));
                view.setBackgroundColor(getResources().getColor(R.color.teal));
                break;
            case 2:
                // Set the imageView to show the respective image
                tutorialImage.setImageResource(R.drawable.onboard_3);

                // Set the textView to show the text
                tutorialTitle.setText(getResources().getString(R.string.tutorial_title_3));
                tutorialCaption.setText(getResources().getString(R.string.tutorial_caption_3));
                view.setBackgroundColor(getResources().getColor(R.color.amber));
        }
        return view;
    }

    /**
     * Returns the page number represented by this fragment object.
     */
    public int getPageNumber() {
        return mPage;
    }
}
