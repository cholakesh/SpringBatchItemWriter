package com.itemwriter.writer;

import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.itemwriter.model.StudentCsv;
import com.itemwriter.model.StudentJdbc;
import com.itemwriter.model.StudentJson;
import com.itemwriter.model.StudentResponse;
import com.itemwriter.model.StudentXml;

@Component
public class FirstItemWriter implements ItemWriter<StudentResponse> {

    @Override
    public void write(List<? extends StudentResponse> items) throws Exception {
        System.out.println("inside Item Writer");
        items.stream().forEach(System.out::println);
    }

}
