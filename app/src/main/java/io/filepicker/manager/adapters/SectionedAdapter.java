package io.filepicker.manager.adapters;

import android.view.View;
import android.widget.ListAdapter;

import com.commonsware.cwac.merge.MergeAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maciejwitowski on 12/8/14.
 */
public class SectionedAdapter {

    private final MergeAdapter mergeAdapter;
    private List<Section> sections;

    public static final int INVALID_POSITION = -1;

    private SectionedAdapter(Builder builder) {
        mergeAdapter = new MergeAdapter();
        sections = builder.sections;

        for(Section section : builder.sections) {
            if(section.adapter.getCount() > 0) {
                mergeAdapter.addView(section.header);
            }
            mergeAdapter.addAdapter(section.adapter);
        }
    }

    public int getPositionInSection(int positionInList, String sectionTag) {
        Section section = getSectionByTag(sectionTag);
        int positionInSection = INVALID_POSITION;

        if(section != null) {
            positionInSection = positionInList - getSectionStart(section);
        }

        return positionInSection;
    }

    public boolean belongsToSection(int positionInList, String sectionTag) {
        Section section = getSectionByTag(sectionTag);

        if(section != null) {
            int sectionStart = getSectionStart(section);
            int sectionEnd = (sectionStart-1) + section.getLength(); // (sectionStart - 1) because we want index

            return positionInList >= sectionStart && positionInList <= sectionEnd;
        }

        return false;
    }

    private int getSectionStart(Section section) {
        int sectionPosition = sections.indexOf(section);
        int count = 1; // starting from 1 because this section has at least 1 element - header
        for(int i = 0; i < sectionPosition; i++) {
            int previousSectionCount = sections.get(i).adapter.getCount();
            if(previousSectionCount > 0) {
                count += 1; // section header
                count += previousSectionCount; // section elements
            }
        }

        return count;
    }

    private Section getSectionByTag(String sectionTag) {
        Section section = null;

        for(Section s : sections) {
            if(s.tag.equals(sectionTag)) {
                section = s;
                break;
            }
        }
        return section;
    }

    public MergeAdapter getAdapter() {
        return mergeAdapter;
    }

    public static class Builder {
        List<Section> sections;

        public Builder() {
            this.sections = new ArrayList<>();
        }

        public Builder add(Section section) {
            this.sections.add(section);
            return this;
        }

        public SectionedAdapter build() {
            return new SectionedAdapter(this);
        }
    }

    public static class Section {
        View header;
        ListAdapter adapter;
        String tag;

        public Section(View header, ListAdapter adapter, String tag) {
            this.header = header;
            this.adapter = adapter;
            this.tag = tag;
        }

        public int getLength() {
            return this.adapter.getCount();
        }
    }
}
