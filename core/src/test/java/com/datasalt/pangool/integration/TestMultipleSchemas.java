package com.datasalt.pangool.integration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.junit.Test;

import com.datasalt.pangolin.commons.HadoopUtils;
import com.datasalt.pangolin.commons.test.AbstractHadoopTestLibrary;
import com.datasalt.pangolin.grouper.GrouperException;
import com.datasalt.pangolin.grouper.io.tuple.BaseTuple;
import com.datasalt.pangolin.grouper.io.tuple.ITuple.InvalidFieldException;
import com.datasalt.pangool.CoGrouper;
import com.datasalt.pangool.CoGrouperException;
import com.datasalt.pangool.PangoolConfig;
import com.datasalt.pangool.PangoolConfigBuilder;
import com.datasalt.pangool.Schema;
import com.datasalt.pangool.SortingBuilder;
import com.datasalt.pangool.mapreduce.GroupHandler;
import com.datasalt.pangool.mapreduce.InputProcessor;
import com.google.common.io.Files;

public class TestMultipleSchemas extends AbstractHadoopTestLibrary {

	public static class FirstInputProcessor extends InputProcessor<LongWritable, Text> {

		@Override
		public void process(LongWritable key, Text value, Collector collector) throws IOException, InterruptedException,
		    GrouperException {

			BaseTuple tuple = new BaseTuple();
			tuple.setString("name", "Pere");
			tuple.setInt("money", 100);
			tuple.setString("country", "ES");

			collector.write(0, tuple);

			tuple.setString("name", "Iván");
			tuple.setInt("money", 50);
			tuple.setString("country", "ES");

			collector.write(0, tuple);

			tuple.setString("name", "Eric");
			tuple.setInt("money", 150);
			tuple.setString("country", "FR");

			collector.write(0, tuple);

			tuple = new BaseTuple();
			tuple.setString("country", "ES");
			tuple.setInt("averageSalary", 1000);

			collector.write(1, tuple);

			tuple.setString("country", "FR");
			tuple.setInt("averageSalary", 1500);
			
			collector.write(1, tuple);
		}
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void test() throws CoGrouperException, InvalidFieldException, IOException, InterruptedException, ClassNotFoundException {
		PangoolConfig config = new PangoolConfigBuilder()
		    .addSchema(0, Schema.parse("name:string, money:int, country:string"))
		    .addSchema(1, Schema.parse("country:string, averageSalary:int")).setGroupByFields("country")
		    .setSorting(new SortingBuilder().add("country").addSourceId().secondarySort(0).add("money").buildSorting())
		    .build();

		Files.write("", new File("test-input"), Charset.forName("UTF-8"));
		
		Job job = new CoGrouper(config, getConf())
		    .addInput(new Path("test-input"), TextInputFormat.class, FirstInputProcessor.class)
		    .setGroupHandler(new GroupHandler() {

			    @Override
			    public void onGroupElements(Iterable tuples, State state, Context context) throws IOException,
			        InterruptedException, CoGrouperException {

				    System.out.println(tuples);
			    }
		    }.getClass()).createJob();

		job.waitForCompletion(true);
		
		HadoopUtils.deleteIfExists(FileSystem.get(getConf()), new Path("test-input"));
	}
}