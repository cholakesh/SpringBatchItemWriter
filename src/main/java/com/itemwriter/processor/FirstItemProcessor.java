package com.itemwriter.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.itemwriter.model.StudentCsv;
import com.itemwriter.model.StudentJdbc;
import com.itemwriter.model.StudentJson;

@Component
public class FirstItemProcessor implements ItemProcessor<StudentCsv, StudentJson> {

    @Override
    @Nullable
    public StudentJson process(@NonNull StudentCsv item) throws Exception {
        if (item.getId() == 6) {
            System.out.println(" Inside the ItemSkipProcessor ");
            throw new NullPointerException();
        }

        StudentJson studentJson = new StudentJson();

        studentJson.setId(item.getId());
        studentJson.setFirstName(item.getFirstName());
        studentJson.setLastName(item.getLastName());
        studentJson.setEmail(item.getEmail());

        return studentJson;
    }

}
