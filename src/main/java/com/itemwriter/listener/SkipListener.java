package com.itemwriter.listener;

import java.io.File;
import java.io.FileWriter;

import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.annotation.OnSkipInRead;
import org.springframework.batch.core.annotation.OnSkipInWrite;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import com.itemwriter.model.StudentCsv;
import com.itemwriter.model.StudentJson;

@Component
public class SkipListener {

    @OnSkipInRead
    public void skipInRead(Throwable th) {
        if (th instanceof FlatFileParseException) {
            createTextFile("demo2/src/main/java/com/itemwriter/ChunkJob/firstChunkStep/reader/skipInRead.txt",
                    ((FlatFileParseException) th).getInput());
        }
    }

    @OnSkipInProcess
    public void skipInProcess(StudentCsv studentCsv, Throwable th) {
        if (th instanceof NullPointerException) {
            createTextFile("demo2/src/main/java/com/itemwriter/ChunkJob/firstChunkStep/processor/skipInProcess.txt",
                    studentCsv.toString());
        }
    }

    @OnSkipInWrite
    public void skipInWrite(StudentJson studentJson, Throwable th) {
        if (th instanceof NullPointerException) {
            createTextFile("demo2/src/main/java/com/itemwriter/ChunkJob/firstChunkStep/writer/skipInWrite.txt",
                    studentJson.toString());
        }
    }

    public void createTextFile(String filePath, String data) {
        try (FileWriter fileWriter = new FileWriter(new File(filePath), true)) {
            fileWriter.write(data + "\n");
        } catch (Exception e) {

        }
    }
}
