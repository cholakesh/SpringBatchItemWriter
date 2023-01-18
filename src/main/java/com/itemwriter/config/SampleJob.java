package com.itemwriter.config;

import java.io.IOException;
import java.io.Writer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.item.adapter.ItemReaderAdapter;
import org.springframework.batch.item.adapter.ItemWriterAdapter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import com.itemwriter.listener.SkipListener;
import com.itemwriter.model.StudentCsv;
import com.itemwriter.model.StudentJdbc;
import com.itemwriter.model.StudentJson;
import com.itemwriter.model.StudentResponse;
import com.itemwriter.model.StudentXml;
import com.itemwriter.processor.FirstItemProcessor;
import com.itemwriter.reader.FirstItemReader;
import com.itemwriter.service.RestApiItemReader;
import com.itemwriter.service.RestApiItemWriter;
import com.itemwriter.writer.FirstItemWriter;

@Configuration
public class SampleJob {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    private FirstItemReader firstItemReader;

    @Autowired
    private FirstItemProcessor firstItemProcessor;

    @Autowired
    private FirstItemWriter firstItemWriter;

    // @Autowired
    // private DataSource dataSource;

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.universitydatasource")
    public DataSource universityDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Autowired
    private RestApiItemReader restApiItemReader;

    @Autowired
    private RestApiItemWriter restApiItemWriter;

    @Autowired
    private SkipListener skipListener;

    @Bean
    public Job chunkJob() {
        return jobBuilderFactory.get("Chunk Job")
                .incrementer(new RunIdIncrementer())
                .start(firstChunkStep())
                .build();
    }

    private Step firstChunkStep() {
        return stepBuilderFactory.get("First Chunk Step")
                // .<StudentCsv, StudentCsv>chunk(3)
                .<StudentCsv, StudentJson>chunk(3)
                .reader(flatFileItemReader(null))
                // .reader(jsonItemReader(null))
                // .reader(staxEventItemReader(null))
                // .reader(jdbcCursorItemReader())
                // .reader(itemReaderAdapter())
                .processor(firstItemProcessor)
                // .writer(flatFileItemWriter(null))
                .writer(jsonFileItemWriter(null))
                // .writer(staxEventItemWriter(null))
                // .writer(jdbcBatchItemWriter())

                // //prepared statement different approach.
                // .writer(jdbcBatchItemWriter1())

                // .writer(itemWriterAdapter())

                // How to skip the bad records for chunk oriented step
                .faultTolerant()
                .skip(Throwable.class) // irrespective of exception -- we can use Throwable.class
                .skipLimit(Integer.MAX_VALUE) // or .skipPolicy(new AlwaysSkipItemSkipPolicy())

                .retryLimit(3) // This limit is applicable to reader and writer and for processor it will be
                               // 3-1=2.
                .retry(Throwable.class)

                .listener(skipListener)
                .build();
    }

    @StepScope
    @Bean // as we are using this @Value annotation this flatFileItemReader needs to be in
          // context
    public FlatFileItemReader<StudentCsv> flatFileItemReader(
            @Value("#{jobParameters['inputFile']}") FileSystemResource fileSystemResource) {
        FlatFileItemReader<StudentCsv> flatFileItemReader = new FlatFileItemReader<StudentCsv>();

        flatFileItemReader.setResource(fileSystemResource);

        flatFileItemReader.setLineMapper(new DefaultLineMapper<StudentCsv>() {
            {
                setLineTokenizer(new DelimitedLineTokenizer() {
                    {
                        setNames("ID", "First Name", "Last Name", "Email"); // order shouldn't change. Should be same
                                                                            // as CSV file
                    }
                });

                setFieldSetMapper(new BeanWrapperFieldSetMapper<StudentCsv>() {
                    {
                        setTargetType(StudentCsv.class);
                    }
                });
            }
        });

        flatFileItemReader.setLinesToSkip(1);
        return flatFileItemReader;
    }

    // @StepScope
    // @Bean
    // public JsonItemReader<StudentJson> jsonItemReader(
    // @Value("#{jobParameters['inputFile']}") FileSystemResource
    // fileSystemResource) {
    // JsonItemReader<StudentJson> jsonItemReader = new JsonItemReader<>();

    // jsonItemReader.setResource(fileSystemResource);

    // jsonItemReader.setJsonObjectReader(new
    // JacksonJsonObjectReader<>(StudentJson.class));

    // jsonItemReader.setMaxItemCount(8); // this is for setting how many items to
    // read from json file.
    // jsonItemReader.setCurrentItemCount(2); // this is used to ignore first 2
    // objects and start from 3. default value
    // // is 0.
    // return jsonItemReader;
    // }

    // @StepScope
    // @Bean
    // public StaxEventItemReader<StudentXml> staxEventItemReader(
    // @Value("#{jobParameters['inputFile']}") FileSystemResource
    // fileSystemResource) {
    // StaxEventItemReader<StudentXml> staxEventItemReader = new
    // StaxEventItemReader<>();

    // staxEventItemReader.setResource(fileSystemResource);

    // staxEventItemReader.setFragmentRootElementName("student");

    // staxEventItemReader.setUnmarshaller(new Jaxb2Marshaller() {
    // {
    // setClassesToBeBound(StudentXml.class);
    // }
    // });

    // return staxEventItemReader;
    // }

    public JdbcCursorItemReader<StudentJdbc> jdbcCursorItemReader() {
        JdbcCursorItemReader<StudentJdbc> jdbcCursorItemReader = new JdbcCursorItemReader<>();

        jdbcCursorItemReader.setDataSource(universityDataSource());

        jdbcCursorItemReader
                .setSql("select Id as id, First_Name, Last_Name as lastName, Email as email from students");

        jdbcCursorItemReader.setRowMapper(new BeanPropertyRowMapper<StudentJdbc>() {
            {
                setMappedClass(StudentJdbc.class);
            }
        });

        // jdbcCursorItemReader.setCurrentItemCount(2);
        // jdbcCursorItemReader.setMaxItemCount(8);
        return jdbcCursorItemReader;
    }

    // public ItemReaderAdapter<StudentResponse> itemReaderAdapter() {
    // ItemReaderAdapter<StudentResponse> itemReaderAdapter = new
    // ItemReaderAdapter<StudentResponse>();

    // itemReaderAdapter.setTargetObject(restApiItemReader);
    // itemReaderAdapter.setTargetMethod("indiviualData");

    // itemReaderAdapter.setArguments(new Object[] { 1L, "Cholakesh" });

    // return itemReaderAdapter;
    // }

    @StepScope
    @Bean
    public FlatFileItemWriter<StudentJdbc> flatFileItemWriter(
            @Value("demo2/src/main/java/com/itemwriter/outputFiles/students.csv") FileSystemResource fileSystemResource) {
        FlatFileItemWriter<StudentJdbc> flatFileItemWriter = new FlatFileItemWriter<>();

        flatFileItemWriter.setResource(fileSystemResource);

        flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback() {

            @Override
            public void writeHeader(Writer writer) throws IOException {
                writer.write("Id,First Name,Last Name,Email");
            }
        });

        flatFileItemWriter.setLineAggregator(new DelimitedLineAggregator<StudentJdbc>() {
            {
                setFieldExtractor(new BeanWrapperFieldExtractor<StudentJdbc>() {
                    {
                        setNames(new String[] { "id", "firstName", "lastName", "email" });
                    }
                });
            }
        });

        flatFileItemWriter.setFooterCallback(new FlatFileFooterCallback() {

            @Override
            public void writeFooter(Writer writer) throws IOException {
                writer.write("created @ " + new Date());

            }

        });

        return flatFileItemWriter;
    }

    @StepScope
    @Bean
    public JsonFileItemWriter<StudentJson> jsonFileItemWriter(
            @Value("#{jobParameters['outputFile']}") FileSystemResource fileSystemResource) {
        JsonFileItemWriter<StudentJson> jsonFileItemWriter = new JsonFileItemWriter<>(fileSystemResource,
                new JacksonJsonObjectMarshaller<StudentJson>()) {
            @Override
            public String doWrite(List<? extends StudentJson> items) {
                items.stream().forEach(item -> {
                    if (item.getId() == 9) {
                        System.out.println(" Inside the ItemSkipWriter ");
                        throw new NullPointerException();
                    }
                });
                return super.doWrite(items);
            }
        };

        return jsonFileItemWriter;
    }

    @StepScope
    @Bean
    public StaxEventItemWriter<StudentJdbc> staxEventItemWriter(
            @Value("demo2/src/main/java/com/itemwriter/outputFiles/students.xml") FileSystemResource fileSystemResource) {
        StaxEventItemWriter<StudentJdbc> staxEventItemWriter = new StaxEventItemWriter<>();

        staxEventItemWriter.setResource(fileSystemResource);
        staxEventItemWriter.setRootTagName("students");

        staxEventItemWriter.setMarshaller(new Jaxb2Marshaller() {
            {
                setClassesToBeBound(StudentJdbc.class);
            }
        });

        return staxEventItemWriter;
    }

    @Bean
    public JdbcBatchItemWriter<StudentCsv> jdbcBatchItemWriter() {
        JdbcBatchItemWriter<StudentCsv> jdbcBatchItemWriter = new JdbcBatchItemWriter<>();

        jdbcBatchItemWriter.setDataSource(universityDataSource());
        jdbcBatchItemWriter.setSql("insert into students values(:id,:firstName,:lastName,:email)");

        jdbcBatchItemWriter
                .setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<StudentCsv>());

        return jdbcBatchItemWriter;
    }

    @Bean
    public JdbcBatchItemWriter<StudentCsv> jdbcBatchItemWriter1() {
        JdbcBatchItemWriter<StudentCsv> jdbcBatchItemWriter = new JdbcBatchItemWriter<>();

        jdbcBatchItemWriter.setDataSource(universityDataSource());
        jdbcBatchItemWriter.setSql("insert into students values(?,?,?,?)");

        jdbcBatchItemWriter.setItemPreparedStatementSetter(new ItemPreparedStatementSetter<StudentCsv>() {

            @Override
            public void setValues(StudentCsv item, PreparedStatement ps) throws SQLException {
                ps.setLong(1, item.getId());
                ps.setString(2, item.getFirstName());
                ps.setString(3, item.getLastName());
                ps.setString(4, item.getEmail());
            }
        });

        return jdbcBatchItemWriter;
    }

    public ItemWriterAdapter<StudentCsv> itemWriterAdapter() {
        ItemWriterAdapter<StudentCsv> itemWriterAdapter = new ItemWriterAdapter<StudentCsv>();

        itemWriterAdapter.setTargetObject(restApiItemWriter);
        itemWriterAdapter.setTargetMethod("createStudent");

        return itemWriterAdapter;
    }
}
