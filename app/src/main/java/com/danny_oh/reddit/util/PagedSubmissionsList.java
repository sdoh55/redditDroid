package com.danny_oh.reddit.util;

import com.github.jreddit.entity.Submission;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by danny on 7/23/14.
 *
 * An extension of ArrayList that supports paging
 */
public class PagedSubmissionsList extends ArrayList<List<Submission>> {
    int mSubmissionsPerPage;
    int mCount;
    int mFirstPageItemCount;    // sometimes there are sticky posts at the top of the returned list

    public PagedSubmissionsList() {
        super();
        mCount = 0;
        mSubmissionsPerPage = 25;
        mFirstPageItemCount = -1;
    }

    public PagedSubmissionsList(int submissionsPerPage) {
        super();
        mCount = 0;
        mSubmissionsPerPage = submissionsPerPage;
    }

    @Override
    public boolean add(List<Submission> object) {
        if (size() == 0) {
            mFirstPageItemCount = object.size();
        }
        mCount += object.size();
        return super.add(object);
    }

    @Override
    public void clear() {
        mCount = 0;
        super.clear();
    }

    public int count() {
        return mCount;
    }

    public Submission getSubmissionAtIndex(int index) {
        if (index < mFirstPageItemCount) {
            return get(0).get(index);
        } else {
            int page = ((index - mFirstPageItemCount) / mSubmissionsPerPage) + 1;
            int indexOnPage = (index - mFirstPageItemCount) - ((page - 1) * mSubmissionsPerPage);

            return get(page).get(indexOnPage);
        }

    }
}
