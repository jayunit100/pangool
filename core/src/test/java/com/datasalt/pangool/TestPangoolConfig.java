package com.datasalt.pangool;

import java.io.IOException;
import java.util.Map;

import junit.framework.Assert;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import com.datasalt.pangolin.grouper.io.tuple.ITuple.InvalidFieldException;
import com.datasalt.pangool.CoGrouperException;
import com.datasalt.pangool.PangoolConfig;
import com.datasalt.pangool.PangoolConfigBuilder;
import com.datasalt.pangool.Schema;
import com.datasalt.pangool.SchemaBuilder;
import com.datasalt.pangool.Sorting;
import com.datasalt.pangool.SortingBuilder;
import com.datasalt.pangool.Schema.Field;
import com.datasalt.pangool.SortCriteria.SortOrder;

public class TestPangoolConfig {

	@Test(expected = CoGrouperException.class)
	public void testSourceIdNotAllowedForOneSchema() throws CoGrouperException {
		/*
		 * We can't add #source# sorting if we only have one schema
		 */
		PangoolConfigBuilder configBuilder = new PangoolConfigBuilder();

		configBuilder.addSchema(0, Schema.parse("url:string, date:long, fetched:long, content:string"));
		configBuilder.setSorting(Sorting.parse("url asc, fetched desc, " + Field.SOURCE_ID_FIELD_NAME + " desc"));
		configBuilder.setGroupByFields("url");
		configBuilder.build();
	}

	@Test(expected = CoGrouperException.class)
	public void testSpecificNotIncludedInCommonSorting() throws CoGrouperException, InvalidFieldException {
		/*
		 * If we sort by url all schemas, we can't sort by url one specific schema
		 */
		PangoolConfigBuilder configBuilder = new PangoolConfigBuilder();

		configBuilder.addSchema(0, Schema.parse("url:string, date:long, fetched:long, content:string"));
		configBuilder.addSchema(1, Schema.parse("fetched:long, url:string, name:string"));
		configBuilder.setSorting(new SortingBuilder().add("url", SortOrder.DESC).addSourceId(SortOrder.ASC)
		    .secondarySort(1).add("url", SortOrder.ASC).buildSorting());
		configBuilder.setGroupByFields("url");
		configBuilder.build();
	}
	
	@Test
	public void testCommonFieldsInSpecificSorting() throws CoGrouperException, InvalidFieldException {
		/*
		 * Sorting by common fields in specific sortings is allowed
		 * Types may differ
		 */
		PangoolConfigBuilder configBuilder = new PangoolConfigBuilder();

		configBuilder.addSchema(0, Schema.parse("url:string, date:long, fetched:long, content:string"));
		configBuilder.addSchema(1, Schema.parse("fetched:vlong, url:string, name:string"));
		configBuilder.setSorting(new SortingBuilder().add("url", SortOrder.DESC).addSourceId(SortOrder.ASC)
		    .secondarySort(1).add("fetched", SortOrder.ASC).buildSorting());
		configBuilder.setGroupByFields("url");
		
		PangoolConfig config = configBuilder.build();
		System.out.println(config.getSorting());
		
		Assert.assertEquals(Schema.parse("url:string, " + Field.SOURCE_ID_FIELD_NAME + ":vint").toString(),
		    config.getCommonOrderedSchema().toString());
		Assert.assertEquals(Schema.parse("fetched:vlong, name:string, url:string").toString(), config.getSpecificOrderedSchemas().get(1).toString());
	}

	@Test
	public void testSourceIdAddedToCommonSchema() throws CoGrouperException {
		PangoolConfigBuilder configBuilder = new PangoolConfigBuilder();

		configBuilder.addSchema(0, Schema.parse("url:string, date:long, fetched:long, content:string"));
		configBuilder.addSchema(1, Schema.parse("fetched:long, url:string, name:string"));
		configBuilder.setSorting(Sorting.parse("url asc, fetched desc"));
		configBuilder.setGroupByFields("url");
		PangoolConfig config = configBuilder.build();

		Assert.assertEquals(Schema.parse("url:string, fetched:long, " + Field.SOURCE_ID_FIELD_NAME + ":vint").toString(),
		    config.getCommonOrderedSchema().toString());
	}

	@Test
	public void testCommonOrderedSchemaWithSourceId() throws InvalidFieldException, CoGrouperException {
		PangoolConfigBuilder configBuilder = new PangoolConfigBuilder();

		configBuilder.addSchema(0, Schema.parse("url:string, date:long, fetched:long, content:string"));
		configBuilder.addSchema(1, Schema.parse("fetched:long, url:string, name:string"));

		configBuilder.setSorting(new SortingBuilder().add("url", SortOrder.ASC).add("fetched", SortOrder.DESC)
		    .addSourceId(SortOrder.ASC).buildSorting());

		configBuilder.setGroupByFields("url");
		PangoolConfig config = configBuilder.build();

		Assert.assertEquals(Schema.parse("url:string, fetched:long, " + Field.SOURCE_ID_FIELD_NAME + ":vint").toString(),
		    config.getCommonOrderedSchema().toString());
	}

	@Test
	public void testParticularPartialOrderedSchemas() throws CoGrouperException {
		PangoolConfigBuilder configBuilder = new PangoolConfigBuilder();

		configBuilder.addSchema(0, Schema.parse("url:string, date:long, fetched:long, content:string"));
		configBuilder.addSchema(1, Schema.parse("fetched:long, url:string, name:string"));
		configBuilder.setSorting(Sorting.parse("url asc, fetched desc"));
		configBuilder.setGroupByFields("url");
		PangoolConfig config = configBuilder.build();

		Map<Integer, Schema> partialOrderedSchemas = config.getSpecificOrderedSchemas();

		Assert.assertEquals(Schema.parse("content:string, date:long").toString(), partialOrderedSchemas.get(0).toString());
		Assert.assertEquals(Schema.parse("name:string").toString(), partialOrderedSchemas.get(1).toString());
	}

	@Test
	public void testSerDeEquality() throws JsonGenerationException, JsonMappingException, IOException,
	    CoGrouperException, InvalidFieldException {
		PangoolConfigBuilder configBuilder = new PangoolConfigBuilder();

		SchemaBuilder builder1 = new SchemaBuilder();
		builder1.add("url", String.class).add("date", Long.class).add("content", String.class);

		SchemaBuilder builder2 = new SchemaBuilder();
		builder2.add("url", String.class).add("date", Long.class).add("name", String.class);

		SortingBuilder builder = new SortingBuilder();
		Sorting sorting = builder.add("url", SortOrder.ASC).add("date", SortOrder.DESC).addSourceId(SortOrder.ASC)
		    .secondarySort(1).add("content", SortOrder.ASC).secondarySort(2).add("name", SortOrder.ASC).buildSorting();

		configBuilder.addSchema(1, builder1.createSchema());
		configBuilder.addSchema(2, builder2.createSchema());
		configBuilder.setSorting(sorting);
		configBuilder.setRollupFrom("url");
		configBuilder.setGroupByFields("url", "date");
		PangoolConfig config = configBuilder.build();

		ObjectMapper mapper = new ObjectMapper();
		String jsonConfig = config.toStringAsJSON(mapper);
		PangoolConfig config2 = PangoolConfigBuilder.fromJSON(jsonConfig, mapper);

		Assert.assertEquals(jsonConfig, config2.toStringAsJSON(mapper));
	}
}