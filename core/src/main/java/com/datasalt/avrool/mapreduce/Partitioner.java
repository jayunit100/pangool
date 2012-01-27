package com.datasalt.avrool.mapreduce;

import java.util.List;

import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapred.AvroValue;
import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;

public class Partitioner extends org.apache.hadoop.mapreduce.Partitioner<AvroKey, AvroValue> implements Configurable {

	private static final String CONF_PARTITIONER_FIELDS = Partitioner.class.getName() + ".partitioner.fields";

	private Configuration conf;
	private String[] groupFields;

	@Override
	public int getPartition(AvroKey key, AvroValue value, int numPartitions) {
		//TODO mimic Record.hashCode
		return 0;
		
		//return key.partialHashCode(groupFields) % numPartitions;
	}

	@Override
	public Configuration getConf() {
		return conf;
	}

	@Override
	public void setConf(Configuration conf) {
		if(conf != null) {
			this.conf = conf;
			String fieldsGroupStr = conf.get(CONF_PARTITIONER_FIELDS);
			groupFields = fieldsGroupStr.split(",");
		}
	}

	public static void setPartitionerFields(Configuration conf, List<String> fields) {
		conf.setStrings(CONF_PARTITIONER_FIELDS, fields.toArray(new String[0]));
	}

	public static String[] getPartitionerFields(Configuration conf) {
		return conf.getStrings(CONF_PARTITIONER_FIELDS);
	}
}