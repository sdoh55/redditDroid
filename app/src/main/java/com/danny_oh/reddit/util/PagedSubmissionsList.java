package com.danny_oh.reddit.util;

import com.github.jreddit.entity.Submission;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by danny on 7/23/14.
 */
public class PagedSubmissionsList extends ArrayList<List<Submission>> {
    int mSubmissionsPerPage;
    int mCount;


    public PagedSubmissionsList() {
        super();
        mCount = 0;
        mSubmissionsPerPage = 25;
    }

    public PagedSubmissionsList(int submissionsPerPage) {
        super();
        mCount = 0;
        mSubmissionsPerPage = submissionsPerPage;
    }

    @Override
    public boolean add(List<Submission> object) {
        mCount += object.size();
        return super.add(object);
    }

    public int count() {
        return mCount;
    }

    public Submission getSubmissionAtIndex(int index) {
        int page = index / mSubmissionsPerPage;
        int indexOnPage = index - (page * mSubmissionsPerPage);

        return get(page).get(indexOnPage);
    }
}
