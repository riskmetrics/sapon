package org.apache.axiom.om.impl;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.TestCase;

import org.apache.axiom.om.impl.serialize.StreamingOMSerializer;

public class StreamingOMSerializerTestCase extends TestCase {
	public void testReaderAttrWithPrefixWriterAttrNull() throws UnsupportedEncodingException, XMLStreamException {
		StreamingOMSerializer s = new StreamingOMSerializer();
		XMLInputFactory xmlif = XMLInputFactory.newInstance();
		XMLOutputFactory xmlof = XMLOutputFactory.newInstance();

		String foo = "<foo xmlns:a=\"http://anamespace\" xml:lang=\"en\">bar</foo>";
		XMLStreamReader node
			= xmlif.createXMLStreamReader(
					new ByteArrayInputStream(foo.getBytes("UTF-8")));
		XMLStreamWriter writer
			= xmlof.createXMLStreamWriter(new StringWriter());

		s.serialize(node, writer);
	}
}
