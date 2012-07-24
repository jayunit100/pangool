/**
 * Copyright [2012] [Datasalt Systems S.L.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datasalt.pangool.tuplemr.mapred.lib.output;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.junit.Test;

import com.datasalt.pangool.BaseTest;
import com.datasalt.pangool.io.ITuple;
import com.datasalt.pangool.io.Schema;
import com.datasalt.pangool.io.Schema.Field;
import com.datasalt.pangool.io.Schema.Field.Type;
import com.datasalt.pangool.tuplemr.IdentityTupleMapper;
import com.datasalt.pangool.tuplemr.IdentityTupleReducer;
import com.datasalt.pangool.tuplemr.TupleMRBuilder;
import com.datasalt.pangool.tuplemr.TupleMRException;
import com.datasalt.pangool.tuplemr.mapred.lib.input.TupleTextInputFormat;
import com.datasalt.pangool.utils.CommonUtils;
import com.datasalt.pangool.utils.HadoopUtils;
import com.google.common.io.Files;

public class TestTupleTextInputOutputFormat extends BaseTest {

	public static String OUT = TestTupleTextInputOutputFormat.class.getName() + "-out";
	public static String IN = TestTupleTextInputOutputFormat.class.getName() + "-in";

	public static enum TestEnum {
		MICKEY, MOUSE, MINIE;
	}

	@SuppressWarnings("rawtypes")
  @Test
	public void test() throws TupleMRException, IOException, InterruptedException, ClassNotFoundException {

		String line1 = "foo1 10.0 bar1 1.0 100 1000000 true MICKEY";
		String line2 = "foo2 20.0 bar2 2.0 200 2000000 false MOUSE";
		String line3 = "foo3 30.0 bar3 3.0 300 3000000 true MINIE";

		// The input is a simple space-separated file with no quotes
		CommonUtils.writeTXT(line1 + "\n" + line2 + "\n" + line3, new File(IN));
		Configuration conf = new Configuration();
		FileSystem fS = FileSystem.get(conf);
		Path outPath = new Path(OUT);
		Path inPath = new Path(IN);
		HadoopUtils.deleteIfExists(fS, outPath);

		// Define the Schema according to the text file
		List<Field> fields = new ArrayList<Field>();
		fields.add(Field.create("strField1", Type.STRING));
		fields.add(Field.create("floatField", Type.FLOAT));
		fields.add(Field.create("strField2", Type.STRING));
		fields.add(Field.create("doubleField", Type.DOUBLE));
		fields.add(Field.create("intField", Type.INT));
		fields.add(Field.create("longField", Type.LONG));
		fields.add(Field.create("booleanField", Type.BOOLEAN));
		fields.add(Field.createEnum("enumField", TestEnum.class));

		Schema schema = new Schema("schema", fields);

		TupleMRBuilder builder = new TupleMRBuilder(conf);
		builder.addIntermediateSchema(schema);
		builder.setGroupByFields("strField1"); // but we don't care, really
		/*
		 * Define the Input Format and the Output Format!
		 */
		InputFormat inputFormat = new TupleTextInputFormat(schema, false, ' ', TupleTextOutputFormat.NO_QUOTE_CHARACTER,
				TupleTextOutputFormat.NO_ESCAPE_CHARACTER);
		OutputFormat outputFormat = new TupleTextOutputFormat(schema, false, ' ', TupleTextOutputFormat.NO_QUOTE_CHARACTER,
				TupleTextOutputFormat.NO_ESCAPE_CHARACTER);

		builder.addInput(inPath, inputFormat, new IdentityTupleMapper());
		builder.setTupleReducer(new IdentityTupleReducer());
		builder.setOutput(outPath, outputFormat, ITuple.class, NullWritable.class);
		builder.createJob().waitForCompletion(true);
		Job job = builder.createJob();
		assertRun(job);

		Assert.assertEquals(line1 + "\n" + line2 + "\n" + line3,
		    Files.toString(new File(OUT + "/" + "part-r-00000"), Charset.forName("UTF-8")).trim());

		HadoopUtils.deleteIfExists(fS, inPath);
		HadoopUtils.deleteIfExists(fS, outPath);
	}
}