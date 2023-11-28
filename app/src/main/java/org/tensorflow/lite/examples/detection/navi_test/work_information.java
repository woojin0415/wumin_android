package org.tensorflow.lite.examples.detection.navi_test;




public class work_information {
    String [][][] informations;
    work_information(int section_size, int numofwork){
        informations = new String[section_size][numofwork][2];
    }

    void set_work(int section, int number, String name, String desc){
        informations[section][number][0] = name;
        informations[section][number][1] = desc;
    }

    String [] get_work(int section, int number){
        return informations[section][number];
    }

}
