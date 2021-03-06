package com.datasalt.pangool.tuplemr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.OutputFormat;

import com.datasalt.pangool.tuplemr.mapred.lib.output.PangoolMultipleOutputs;
import com.datasalt.pangool.tuplemr.mapred.lib.output.TupleOutputFormat;
import com.datasalt.pangool.utils.AvroUtils;

/**
 * This class encapsulates the functionality of a builder such as {@link TupleMRBuilder} that provides Named Outputs.
 * To be used by other builders like {@link MapOnlyJobBuilder}.
 */
@SuppressWarnings("rawtypes")
public class NamedOutputsInterface {

	private final Configuration conf;

	public NamedOutputsInterface(Configuration conf) {
		this.conf = conf;
	}

	public static final class Output {

		String name;
    OutputFormat outputFormat;
		Class keyClass;
		Class valueClass;

		Map<String, String> specificContext = new HashMap<String, String>();

		Output(String name, OutputFormat outputFormat, Class keyClass, Class valueClass,
		    Map<String, String> specificContext) {
			this.outputFormat = outputFormat;
			this.keyClass = keyClass;
			this.valueClass = valueClass;
			this.name = name;
			if(specificContext != null) {
				this.specificContext = specificContext;
			}
		}
	}

	private List<Output> namedOutputs = new ArrayList<Output>();

	public void add(Output output) throws TupleMRException {
		NamedOutputsInterface.validateNamedOutput(output.name, namedOutputs);
		if(output.outputFormat.getClass().equals(TupleOutputFormat.class)) {
			AvroUtils.addAvroSerialization(conf);
		}
		namedOutputs.add(output);
	}
	
	public List<Output> getNamedOutputs() {
  	return namedOutputs;
  }
	
	/**
	 * Use this method for configuring a Job instance according to the named outputs specs that has been specified.
	 */
	public void configureJob(Job job) throws FileNotFoundException, IOException, TupleMRException {
		for(Output output : getNamedOutputs()) {
			try {
				PangoolMultipleOutputs.addNamedOutput(job, output.name, output.outputFormat,
				    output.keyClass, output.valueClass);
			} catch(URISyntaxException e1) {
				throw new TupleMRException(e1);
			}
			for(Map.Entry<String, String> contextKeyValue : output.specificContext.entrySet()) {
				PangoolMultipleOutputs.addNamedOutputContext(job, output.name,
				    contextKeyValue.getKey(), contextKeyValue.getValue());
			}
		}
	}

	private static void validateNamedOutput(String namedOutput, List<Output> namedOutputs)
	    throws TupleMRException {
		PangoolMultipleOutputs.validateOutputName(namedOutput);
		for(Output existentNamedOutput : namedOutputs) {
			if(existentNamedOutput.name.equals(namedOutput)) {
				throw new TupleMRException("Duplicate named output: " + namedOutput);
			}
		}
	}
}