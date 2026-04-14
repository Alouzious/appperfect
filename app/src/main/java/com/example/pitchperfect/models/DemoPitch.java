package com.example.pitchperfect.models;

import com.example.pitchperfect.data.MockDataRepository.DemoPitchData;

public class DemoPitch {
    private String id;
    private String title;
    private String description;
    private String category;

    public DemoPitch(String id, String title, String description, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public static DemoPitch[] getDemoPitches() {
        return new DemoPitch[]{
                new DemoPitch("demo_1", 
                    DemoPitchData.TITLES[0],
                    DemoPitchData.DESCRIPTIONS[0],
                    DemoPitchData.CATEGORIES[0]),
                new DemoPitch("demo_2", 
                    DemoPitchData.TITLES[1],
                    DemoPitchData.DESCRIPTIONS[1],
                    DemoPitchData.CATEGORIES[1]),
                new DemoPitch("demo_3", 
                    DemoPitchData.TITLES[2],
                    DemoPitchData.DESCRIPTIONS[2],
                    DemoPitchData.CATEGORIES[2]),
                new DemoPitch("demo_4", 
                    DemoPitchData.TITLES[3],
                    DemoPitchData.DESCRIPTIONS[3],
                    DemoPitchData.CATEGORIES[3])
        };
    }
}
