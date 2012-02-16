/**
 * Copyright [2011] [Datasalt Systems S.L.]
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
package com.datasalt.pangool.mapreduce;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configurable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datasalt.pangool.CoGrouperConfig;
import com.datasalt.pangool.CoGrouperException;
import com.datasalt.pangool.SerializationInfo;
import com.datasalt.pangool.api.CombinerHandler;
import com.datasalt.pangool.api.CombinerHandler.CoGrouperContext;
import com.datasalt.pangool.api.CombinerHandler.Collector;
import com.datasalt.pangool.commons.DCUtils;
import com.datasalt.pangool.io.tuple.DatumWrapper;
import com.datasalt.pangool.io.tuple.ViewTuple;
import com.datasalt.pangool.io.tuple.ITuple;

public class SimpleCombiner extends Reducer<DatumWrapper<ITuple>, NullWritable,DatumWrapper<ITuple>, NullWritable> {

	public final static String CONF_COMBINER_HANDLER = SimpleCombiner.class.getName() + ".combiner.handler";
	private final static Logger log = LoggerFactory.getLogger(SimpleCombiner.class);
	
	// Following variables protected to be shared by Combiners
	private CoGrouperConfig grouperConfig;
	private SerializationInfo serInfo;
	private TupleIterator<DatumWrapper<ITuple>, NullWritable> grouperIterator;
	private ViewTuple groupTuple; // Tuple view over the group
	private CoGrouperContext context;
	private CombinerHandler handler;
	private Collector collector;
	private boolean isMultipleSources;

	public void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		try {
			log.info("Getting CoGrouper grouperConf.");
			this.grouperConfig = CoGrouperConfig.get(context.getConfiguration());
			this.serInfo = this.grouperConfig.getSerializationInfo();
			this.isMultipleSources = this.grouperConfig.getNumSources() >= 2;
			log.info("Getting CoGrouper grouperConf done.");
			if (isMultipleSources){
				this.groupTuple = new ViewTuple(serInfo.getGroupSchema());
			} else {
				this.groupTuple = new ViewTuple(serInfo.getGroupSchema(),serInfo.getGroupSchemaIndexTranslation(0));
			}
			this.grouperIterator = new TupleIterator<DatumWrapper<ITuple>, NullWritable>(context);

			String fileName = context.getConfiguration().get(SimpleCombiner.CONF_COMBINER_HANDLER);
			handler = DCUtils.loadSerializedObjectInDC(context.getConfiguration(), CombinerHandler.class, fileName);
			if(handler instanceof Configurable) {
				((Configurable) handler).setConf(context.getConfiguration());
			}
			collector = new Collector(grouperConfig, context);
			this.context = handler.new CoGrouperContext(context, grouperConfig);
			handler.setup(this.context, collector);
		} catch(CoGrouperException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void cleanup(Context context) throws IOException, InterruptedException {
		try {

			handler.cleanup(this.context, collector);
			super.cleanup(context);
		} catch(CoGrouperException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final void reduce(DatumWrapper<ITuple> key, Iterable<NullWritable> values, Context context) throws IOException,
	    InterruptedException {
		try {
			Iterator<NullWritable> iterator = values.iterator();
			grouperIterator.setIterator(iterator);

			// We get the firts tuple, to create the groupTuple view
			ITuple firstTupleGroup = key.datum();

			// A view is created over the first tuple to give the user the group fields
			if (isMultipleSources){ //TODO consider not using translation here
				int sourceId = grouperConfig.getSourceIdByName(firstTupleGroup.getSchema().getName());
				int[] indexTranslation = serInfo.getGroupSchemaIndexTranslation(sourceId);
				groupTuple.setContained(firstTupleGroup,indexTranslation);
			} else {
				groupTuple.setContained(firstTupleGroup);
			}
			handler.onGroupElements(groupTuple, grouperIterator, this.context, collector);

		} catch(CoGrouperException e) {
			throw new RuntimeException(e);
		}
	}
}