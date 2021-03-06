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
package com.datasalt.pangool.flow;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import com.datasalt.pangool.io.ITuple;
import com.datasalt.pangool.io.Schema;
import com.datasalt.pangool.io.Schema.Field;
import com.datasalt.pangool.io.Tuple;
import com.datasalt.pangool.tuplemr.TupleMapper.TupleMRContext;
import com.datasalt.pangool.utils.HadoopUtils;

public class Utils {

	public final static Tuple cacheTuple(Tuple tuple, @SuppressWarnings("rawtypes") TupleMRContext context, String schemaName) {
		if(tuple == null) {
			tuple = new Tuple(context.getTupleMRConfig().getIntermediateSchema(schemaName));
		}
		return tuple;
	}
	
	public static void shallowCopy(ITuple tupleOrig, ITuple tupleDest, Schema copySchema) {
		for(Field field: copySchema.getFields()) {
			tupleDest.set(field.getName(), tupleOrig.get(field.getName()));
		}
	}
	
	public static void delete(Path path, Configuration conf) throws IOException {
		HadoopUtils.deleteIfExists(path.getFileSystem(conf), path);
	}
}