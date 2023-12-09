package org.tensorflow.lite.examples.detection.Backup;




public class work_information {
    String [][][] informations;
    work_information(int section_size, int numofwork){
        informations = new String[section_size][numofwork][3];
    }

    void set_work(int section, int number, String name, String artist, String desc){
        informations[section][number][0] = name;
        informations[section][number][1] = artist;
        informations[section][number][2] = desc;
    }

    String [] get_work(int section, int number){
        return informations[section][number];
    }

}
