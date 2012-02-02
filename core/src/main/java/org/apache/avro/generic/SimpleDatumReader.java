package org.apache.avro.generic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;

public class SimpleDatumReader<D> implements DatumReader<D>{
	
	
	
	
	
	  private final GenericData data;
	  private Schema schema;
	  //private Schema expected;
	  
	  //private ResolvingDecoder creatorResolver = null;
	  //private final Thread creator;

	  public SimpleDatumReader() {
	    this(null,GenericData.get());
	  }

	  /** Construct where the writer's and reader's schemas are the same. */
	  public SimpleDatumReader(Schema schema) {
	    this(schema, GenericData.get());
	  }

//	  /** Construct given writer's and reader's schema. */
//	  public SimpleDatumReader(Schema writer, Schema reader) {
//	    this(writer, reader, GenericData.get());
//	  }

	  protected SimpleDatumReader(Schema schema, GenericData data) {
	    this.schema = schema;
	    this.data = data;
	    //this.creator = Thread.currentThread();
	  }

	  /** Return the {@link GenericData} implementation. */
	  public GenericData getData() { return data; }

	  /** Return the writer's schema. */
	  public Schema getSchema() { return schema; }

	  @Override
	  public void setSchema(Schema schema) {
	    this.schema = schema;
//	    if (expected == null) {
//	      expected = schema;
//	    }
//	    creatorResolver = null;
	  }

//	  /** Get the reader's schema. */
//	  public Schema getExpected() { return expected; }
//
//	  /** Set the reader's schema. */
//	  public void setExpected(Schema reader) {
//	    this.expected = reader;
//	    creatorResolver = null;
//	  }

//	  private static final ThreadLocal<Map<Schema,Map<Schema,ResolvingDecoder>>>
//	    RESOLVER_CACHE =
//	    new ThreadLocal<Map<Schema,Map<Schema,ResolvingDecoder>>>() {
//	    protected Map<Schema,Map<Schema,ResolvingDecoder>> initialValue() {
//	      return new WeakIdentityHashMap<Schema,Map<Schema,ResolvingDecoder>>();
//	    }
//	  };

//	  /** Gets a resolving decoder for use by this GenericDatumReader.
//	   *  Unstable API.
//	   *  Currently uses a thread local cache to prevent constructing the
//	   *  resolvers too often, because that is very expensive.
//	   */
//	  protected final ResolvingDecoder getResolver(Schema schema, Schema expected)
//	    throws IOException {
//	    Thread currThread = Thread.currentThread();
//	    ResolvingDecoder resolver;
//	    if (currThread == creator && creatorResolver != null) {
//	      return creatorResolver;
//	    } 
//
//	    Map<Schema,ResolvingDecoder> cache = RESOLVER_CACHE.get().get(schema);
//	    if (cache == null) {
//	      cache = new WeakIdentityHashMap<Schema,ResolvingDecoder>();
//	      RESOLVER_CACHE.get().put(schema, cache);
//	    }
//	    resolver = cache.get(expected);
//	    if (resolver == null) {
//	      resolver = DecoderFactory.get().resolvingDecoder(
//	          Schema.applyAliases(schema, expected), expected, null);
//	      cache.put(expected, resolver);
//	    }
//	    
//	    if (currThread == creator){
//	      creatorResolver = resolver;
//	    }
//
//	    return resolver;
//	  }

	  @Override
	  @SuppressWarnings("unchecked")
	  public D read(D reuse, Decoder in) throws IOException {
	   // ResolvingDecoder resolver = getResolver(schema, expected);
	    //resolver.configure(in);
	    D result = (D) read(reuse, schema, in);
	    //resolver.drain();
	    return result;
	  }
		
	  /** Called to read data.*/
	  protected Object read(Object old, Schema expected,
	      Decoder in) throws IOException {
	    switch (expected.getType()) {
	    case RECORD:  return readRecord(old, expected, in);
	    case ENUM:    return readEnum(expected, in);
	    case ARRAY:   return readArray(old, expected, in);
	    case MAP:     return readMap(old, expected, in);
	    case UNION:   return read(old, expected.getTypes().get(in.readIndex()), in);
	    case FIXED:   return readFixed(old, expected, in);
	    case STRING:  return readString(old, expected, in);
	    case BYTES:   return readBytes(old, in);
	    case INT:     return readInt(old, expected, in);
	    case LONG:    return in.readLong();
	    case FLOAT:   return in.readFloat();
	    case DOUBLE:  return in.readDouble();
	    case BOOLEAN: return in.readBoolean();
	    case NULL:    in.readNull(); return null;
	    default: throw new AvroRuntimeException("Unknown type: " + expected);
	    }
	  }

	  /** Called to read a record instance. May be overridden for alternate record
	   * representations.*/
	  protected Object readRecord(Object old, Schema schema, 
	      Decoder in) throws IOException {
	    Object r = data.newRecord(old, schema);
	    Object state = data.getRecordState(r, schema);
	    
	    for (Field f : schema.getFields()) {
	      int pos = f.pos();
	      String name = f.name();
	      Object oldDatum = (old!=null) ? data.getField(r, name, pos, state) : null;
	      data.setField(r, name, pos, read(oldDatum, f.schema(), in), state);
	    }

	    return r;
	  }
	  
	  //@Override
	  @SuppressWarnings("unchecked")
	  protected Object createEnum(String symbol, Schema schema) {
	    Class c = ((SpecificData)getData()).getClass(schema);
	    if (c == null) return superCreateEnum(symbol, schema); // punt to generic
	    return Enum.valueOf(c, symbol);
	  }
	  
	  /** Called to create an enum value. May be overridden for alternate enum
	   * representations.  By default, returns a GenericEnumSymbol. */
	  protected Object superCreateEnum(String symbol, Schema schema) {
	    return new GenericData.EnumSymbol(schema, symbol);
	  }
	  
	  /** Called to read an enum value. May be overridden for alternate enum
	   * representations.  By default, returns a GenericEnumSymbol. */
	  protected Object readEnum(Schema expected, Decoder in) throws IOException {
	    return createEnum(expected.getEnumSymbols().get(in.readEnum()), expected);
	  }

	  

	  /** Called to read an array instance.  May be overridden for alternate array
	   * representations.*/
	  protected Object readArray(Object old, Schema expected,
	      Decoder in) throws IOException {
	    Schema expectedType = expected.getElementType();
	    long l = in.readArrayStart();
	    long base = 0;
	    if (l > 0) {
	      Object array = newArray(old, (int) l, expected);
	      do {
	        for (long i = 0; i < l; i++) {
	          addToArray(array, base + i, read(peekArray(array), expectedType, in));
	        }
	        base += l;
	      } while ((l = in.arrayNext()) > 0);
	      return array;
	    } else {
	      return newArray(old, 0, expected);
	    }
	  }

	  /** Called by the default implementation of {@link #readArray} to retrieve a
	   * value from a reused instance.  The default implementation is for {@link
	   * GenericArray}.*/
	  @SuppressWarnings("unchecked")
	  protected Object peekArray(Object array) {
	    return (array instanceof GenericArray)
	      ? ((GenericArray)array).peek()
	      : null;
	  }

	  /** Called by the default implementation of {@link #readArray} to add a
	   * value.  The default implementation is for {@link Collection}.*/
	  @SuppressWarnings("unchecked")
	  protected void addToArray(Object array, long pos, Object e) {
	    ((Collection) array).add(e);
	  }
	  
	  /** Called to read a map instance.  May be overridden for alternate map
	   * representations.*/
	  protected Object readMap(Object old, Schema expected,
	      Decoder in) throws IOException {
	    Schema eValue = expected.getValueType();
	    long l = in.readMapStart();
	    Object map = newMap(old, (int) l);
	    if (l > 0) {
	      do {
	        for (int i = 0; i < l; i++) {
	          addToMap(map, readString(null, expected, in), read(null, eValue, in));
	        }
	      } while ((l = in.mapNext()) > 0);
	    }
	    return map;
	  }

	  /** Called by the default implementation of {@link #readMap} to add a
	   * key/value pair.  The default implementation is for {@link Map}.*/
	  @SuppressWarnings("unchecked")
	  protected void addToMap(Object map, Object key, Object value) {
	    ((Map) map).put(key, value);
	  }
	  
	  /** Called to read a fixed value. May be overridden for alternate fixed
	   * representations.  By default, returns {@link GenericFixed}. */
	  protected Object readFixed(Object old, Schema expected, Decoder in)
	    throws IOException {
	    GenericFixed fixed = (GenericFixed)data.createFixed(old, expected);
	    in.readFixed(fixed.bytes(), 0, expected.getFixedSize());
	    return fixed;
	  }
	  
//	  /** 
//	   * Called to create an fixed value. May be overridden for alternate fixed
//	   * representations.  By default, returns {@link GenericFixed}.
//	   * @deprecated As of Avro 1.6.0 this method has been moved to 
//	   * {@link GenericData#createFixed(Object, Schema)}
//	   */
//	  @Deprecated
//	  protected Object createFixed(Object old, Schema schema) {
//	    return data.createFixed(old, schema);
//	  }
//
//	  /** 
//	   * Called to create an fixed value. May be overridden for alternate fixed
//	   * representations.  By default, returns {@link GenericFixed}.
//	   * @deprecated As of Avro 1.6.0 this method has been moved to 
//	   * {@link GenericData#createFixed(Object, byte[], Schema)}
//	   */
//	  @Deprecated
//	  protected Object createFixed(Object old, byte[] bytes, Schema schema) {
//	    return data.createFixed(old, bytes, schema);
//	  }
//	  
//	  /**
//	   * Called to create new record instances. Subclasses may override to use a
//	   * different record implementation. The returned instance must conform to the
//	   * schema provided. If the old object contains fields not present in the
//	   * schema, they should either be removed from the old object, or it should
//	   * create a new instance that conforms to the schema. By default, this returns
//	   * a {@link GenericData.Record}.
//	   * @deprecated As of Avro 1.6.0 this method has been moved to 
//	   * {@link GenericData#newRecord(Object, Schema)}
//	   */
//	  @Deprecated
//	  protected Object newRecord(Object old, Schema schema) {
//	    return data.newRecord(old, schema);
//	  }

	  /** Called to create new array instances.  Subclasses may override to use a
	   * different array implementation.  By default, this returns a {@link
	   * GenericData.Array}.*/
	  @SuppressWarnings("unchecked")
	  protected Object newArray(Object old, int size, Schema schema) {
	    if (old instanceof Collection) {
	      ((Collection) old).clear();
	      return old;
	    } else return new GenericData.Array(size, schema);
	  }

	  /** Called to create new array instances.  Subclasses may override to use a
	   * different map implementation.  By default, this returns a {@link
	   * HashMap}.*/
	  @SuppressWarnings("unchecked")
	  protected Object newMap(Object old, int size) {
	    if (old instanceof Map) {
	      ((Map) old).clear();
	      return old;
	    } else return new HashMap<Object, Object>(size);
	  }

	  /** Called to read strings.  Subclasses may override to use a different
	   * string representation.  By default, this calls {@link
	   * #readString(Object,Decoder)}.*/
	  protected Object readString(Object old, Schema expected,
	                              Decoder in) throws IOException {
//	    if (data.STRING_TYPE_STRING.equals(expected.getProp(data.STRING_PROP)))
//	      return in.readString();
//	    else
	      return readString(old, in);
	  }                  

	  /** Called to read strings.  Subclasses may override to use a different
	   * string representation.  By default, this calls {@link
	   * Decoder#readString(Utf8)}.*/
	  protected Object readString(Object old, Decoder in) throws IOException {
	    return in.readString(old instanceof Utf8 ? (Utf8)old : null);
	  }

	  /** Called to create a string from a default value.  Subclasses may override
	   * to use a different string representation.  By default, this calls {@link
	   * Utf8#Utf8(String)}.*/
	  protected Object createString(String value) { return new Utf8(value); }

	  /** Called to read byte arrays.  Subclasses may override to use a different
	   * byte array representation.  By default, this calls {@link
	   * Decoder#readBytes(ByteBuffer)}.*/
	  protected Object readBytes(Object old, Decoder in) throws IOException {
	    return in.readBytes(old instanceof ByteBuffer ? (ByteBuffer) old : null);
	  }

	  /** Called to read integers.  Subclasses may override to use a different
	   * integer representation.  By default, this calls {@link
	   * Decoder#readInt()}.*/
	  protected Object readInt(Object old, Schema expected, Decoder in)
	    throws IOException {
	    return in.readInt();
	  }

	  /** Called to create byte arrays from default values.  Subclasses may
	   * override to use a different byte array representation.  By default, this
	   * calls {@link ByteBuffer#wrap(byte[])}.*/
	  protected Object createBytes(byte[] value) { return ByteBuffer.wrap(value); }

	  /** Skip an instance of a schema. */
	  public static void skip(Schema schema, Decoder in) throws IOException {
	    switch (schema.getType()) {
	    case RECORD:
	      for (Field field : schema.getFields())
	        skip(field.schema(), in);
	      break;
	    case ENUM:
	      in.readInt();
	      break;
	    case ARRAY:
	      Schema elementType = schema.getElementType();
	      for (long l = in.skipArray(); l > 0; l = in.skipArray()) {
	        for (long i = 0; i < l; i++) {
	          skip(elementType, in);
	        }
	      }
	      break;
	    case MAP:
	      Schema value = schema.getValueType();
	      for (long l = in.skipMap(); l > 0; l = in.skipMap()) {
	        for (long i = 0; i < l; i++) {
	          in.skipString();
	          skip(value, in);
	        }
	      }
	      break;
	    case UNION:
	      skip(schema.getTypes().get((int)in.readIndex()), in);
	      break;
	    case FIXED:
	      in.skipFixed(schema.getFixedSize());
	      break;
	    case STRING:
	      in.skipString();
	      break;
	    case BYTES:
	      in.skipBytes();
	      break;
	    case INT:     in.readInt();           break;
	    case LONG:    in.readLong();          break;
	    case FLOAT:   in.readFloat();         break;
	    case DOUBLE:  in.readDouble();        break;
	    case BOOLEAN: in.readBoolean();       break;
	    case NULL:                            break;
	    default: throw new RuntimeException("Unknown type: "+schema);
	    }
	  }

	}


	
	

