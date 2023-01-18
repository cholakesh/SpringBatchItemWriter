package com.itemwriter.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.itemwriter.model.StudentCsv;
import com.itemwriter.model.StudentResponse;

@Service
public class RestApiItemWriter {

    public StudentResponse createStudent(StudentCsv studentCsv) {
        RestTemplate restTemplate = new RestTemplate();
        System.out.println("After resttemplate is declared ");
        return restTemplate.postForObject("http://localhost:8081/api/v1/createStudent",
                studentCsv,
                StudentResponse.class);
    }

}
