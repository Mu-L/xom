/* Copyright 2002-2005 Elliotte Rusty Harold
   
   This library is free software; you can redistribute it and/or modify
   it under the terms of version 2.1 of the GNU Lesser General Public 
   License as published by the Free Software Foundation.
   
   This library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
   GNU Lesser General Public License for more details.
   
   You should have received a copy of the GNU Lesser General Public
   License along with this library; if not, write to the 
   Free Software Foundation, Inc., 59 Temple Place, Suite 330, 
   Boston, MA 02111-1307  USA
   
   You can contact Elliotte Rusty Harold by sending e-mail to
   elharo@metalab.unc.edu. Please include the word "XOM" in the
   subject line. The XOM home page is located at http://www.xom.nu/
*/

package nu.xom.tests;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ibm.icu.text.Normalizer;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Comment;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Namespace;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;
import nu.xom.XPathContext;
import nu.xom.canonical.CanonicalizationException;
import nu.xom.canonical.Canonicalizer;

/**
 * <p>
 *  Tests canonicalization.
 * </p>
 * 
 * @author Elliotte Rusty Harold
 * @version 1.1d7
 *
 */
public class CanonicalizerTest extends XOMTestCase {

    private final static double version = Double.parseDouble(
      System.getProperty("java.version").substring(0,3)
    );
    
    private File canonical;
    private File input;
    private File output;
    
    public CanonicalizerTest(String name) {
        super(name);
    }

    
    private Builder builder = new Builder(); 
    
    
    protected void setUp() { 
        File data = new File("data");
        canonical = new File(data, "canonical");
        input = new File(canonical, "input");
        output = new File(canonical, "output");
    }
    
    
    public void testCanonicalizeOnlyAttributes() throws IOException {
        
        Element pdu = new Element("doc");
        pdu.addAttribute(new Attribute("a1", "v1"));
        pdu.addAttribute(new Attribute("a2", "v2"));
        
        String expected = " a1=\"v1\" a2=\"v2\"";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out);
        
        Document doc = new Document(pdu);
        canonicalizer.write(doc.query("//@*"));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }

    
    public void testRemoveDuplicateAttributes() throws IOException {
        
        Element pdu = new Element("doc");
        Attribute a1 = new Attribute("a1", "v1");
        pdu.addAttribute(a1);
        Attribute a2 = new Attribute("a2", "v2");
        pdu.addAttribute(a2);
        
        String expected = " a1=\"v1\" a2=\"v2\"";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out);
        
        Document doc = new Document(pdu);
        Nodes subset = doc.query("//@*");
        subset.append(a1);
        subset.append(a2);
        canonicalizer.write(subset);  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }

    
    public void testCanonicalizeOnlyNamespaces() throws IOException {
        
        Element pdu = new Element("doc", "http://www.example.com");
        
        String expected = " xmlns=\"http://www.example.com\"";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out);
        
        Document doc = new Document(pdu);
        canonicalizer.write(doc.query("//namespace::node()"));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }

    
    public void testCanonicalizeOnlyPrefixedNamespaces() 
      throws IOException {
        
        Element pdu = new Element("pre:doc", "http://www.example.com");
        
        String expected = " xmlns:pre=\"http://www.example.com\"";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out);
        
        Document doc = new Document(pdu);
        canonicalizer.write(doc.query("//namespace::node()"));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }

    
    public void testCanonicalizeWithNullAlgorithm() 
      throws IOException {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new Canonicalizer(out, null);
            fail("Allowed null algorithm");
        }
        catch (NullPointerException success) {
            assertNotNull(success.getMessage());
        }
        
    }

    
    public void testCanonicalizeCommentsInPrologAndEpilog() throws IOException {
        
        Element pdu = new Element("doc");
        
        Document doc = new Document(pdu);
        doc.insertChild(new Comment("comment 1"), 0);
        doc.insertChild(new Comment("comment 2"), 1);
        doc.appendChild(new Comment("comment 3"));
        doc.appendChild(new Comment("comment 4"));
        
        String expected = "<!--comment 1-->\n<!--comment 2-->\n\n<!--comment 3-->\n<!--comment 4-->";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out);
        
        canonicalizer.write(doc.query("//comment()"));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }

    
    public void testCanonicalizePrologAndEpilog() throws IOException {
        
        Element pdu = new Element("doc");
        
        Document doc = new Document(pdu);
        doc.insertChild(new ProcessingInstruction("target", "value"), 0);
        doc.insertChild(new Comment("comment 2"), 1);
        doc.appendChild(new Comment("comment 3"));
        doc.appendChild(new ProcessingInstruction("target", "value"));
        
        String expected = "<?target value?>\n<!--comment 2-->\n<doc></doc>\n<!--comment 3-->\n<?target value?>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out);
        
        canonicalizer.write(doc);  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }

    
    public void testCanonicalizeOnlyAttributesOnDifferentElements() 
      throws IOException {
        
        Element pdu = new Element("doc");
        pdu.addAttribute(new Attribute("a2", "v1"));
        Element child = new Element("child");
        child.addAttribute(new Attribute("a1", "v2"));
        pdu.appendChild(child);
        
        String expected = " a2=\"v1\" a1=\"v2\"";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out);
        
        Document doc = new Document(pdu);
        canonicalizer.write(doc.query("//@*"));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }

    
    public void testCanonicalizeAttributesWithFunkyCharacters() 
      throws IOException {
        
        Element pdu = new Element("doc");
        pdu.addAttribute(new Attribute("a2", "v1&<>\"\t\r\n"));
        
        String expected = " a2=\"v1&amp;&lt;>&quot;&#x9;&#xD;&#xA;\"";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out);
        
        Document doc = new Document(pdu);
        canonicalizer.write(doc.query("//@*"));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }

    
    public void testExclusiveEmptyRootElementInNoNamespace() 
      throws IOException {
     
        Element pdu = new Element("doc");
   
        String expected = "<doc></doc>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out, 
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        Document doc = new Document(pdu);
        canonicalizer.write(doc);  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
        

    public void testExclusiveEmptyRootElementInNoNamespaceWithTwoAttributes() 
      throws IOException {
     
        Element pdu = new Element("doc");
        pdu.addAttribute(new Attribute("a1", "v1"));
        pdu.addAttribute(new Attribute("a2", "v2"));
        
        String expected = "<doc a1=\"v1\" a2=\"v2\"></doc>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        Document doc = new Document(pdu);
        canonicalizer.write(doc);  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
        

    public void testExclusiveDoesntRenderUnusedPrefix() 
      throws IOException {
     
        Element pdu = new Element("n0:tuck", "http://a.example");
        pdu.addNamespaceDeclaration("pre", "http://www.example.org/");
   
        String expected = "<n0:tuck xmlns:n0=\"http://a.example\"></n0:tuck>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        Document doc = new Document(pdu);
        canonicalizer.write(doc);  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
    
    
    public void testWriteDefaultNamespace() throws IOException {

        Element pdu = new Element("tuck", "http://www.example.org/");
        
        String expected = " xmlns=\"http://www.example.org/\"";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        Document doc = new Document(pdu);
        Nodes subset = doc.query("//namespace::node()");
        canonicalizer.write(subset);  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
        

    public void testOutputAncestorAttributeAndChildHaveDifferentLanguages() 
      throws IOException {
     
        Element pdu = new Element("tuck");
        pdu.addAttribute(new Attribute("xml:lang", Namespace.XML_NAMESPACE, "fr"));
        
        Element middle = new Element("middle");
        pdu.appendChild(middle);
        Element child = new Element("child");
        child.addAttribute(new Attribute("xml:lang", Namespace.XML_NAMESPACE, "en"));
        middle.appendChild(child);
        
        String expected = "<tuck xml:lang=\"fr\"><child xml:lang=\"en\"></child></tuck>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        Document doc = new Document(pdu);
        Nodes subset = doc.query("/* | //child | //@*");
        canonicalizer.write(subset);  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
        

    public void testOutputAncestorAttributeUsesPrefix() 
      throws IOException {
     
        Element pdu = new Element("tuck");
        pdu.addAttribute(new Attribute("pre:foo", "http://www.example.org/", "value"));
        Element child = new Element("pre:test", "http://www.example.org/");
        pdu.appendChild(child);
        
        String expected = "<tuck xmlns:pre=\"http://www.example.org/\" pre:foo=\"value\"><pre:test></pre:test></tuck>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        Document doc = new Document(pdu);
        canonicalizer.write(doc);  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
        

    public void testOutputAncestorAttributeRedefinesPrefix() 
      throws IOException {
     
        Element pdu = new Element("tuck");
        pdu.addAttribute(new Attribute("pre:foo", "http://www.example.com/", "value"));
        Element child = new Element("pre:test", "http://www.example.org/");
        pdu.appendChild(child);
        
        String expected = "<tuck xmlns:pre=\"http://www.example.com/\" pre:foo=\"value\"><pre:test xmlns:pre=\"http://www.example.org/\"></pre:test></tuck>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        Document doc = new Document(pdu);
        canonicalizer.write(doc);  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
        

    public void testExclusiveDoesntRenderUnusedPrefixFromUnincludedAttribute() 
      throws IOException {
     
        Element pdu = new Element("n0:tuck", "http://a.example");
        pdu.addAttribute(new Attribute("pre:foo", "http://www.example.org/", "test"));
   
        String expected = "<n0:tuck xmlns:n0=\"http://a.example\"></n0:tuck>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out, 
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        Document doc = new Document(pdu);
        canonicalizer.write(doc.query("//* | //namespace::node()"));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
        

    public void testWithComments() 
      throws ParsingException, IOException {
      
        File tests = input;
        String[] inputs = tests.list(new XMLFilter());
        for (int i = 0; i < inputs.length; i++) {
            File input = new File(tests, inputs[i]);   
            Document doc = builder.build(input);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                Canonicalizer serializer = new Canonicalizer(out);
                serializer.write(doc);
            }
            finally {
                out.close();
            }            
            byte[] actual = out.toByteArray();
            
            // for debugging
            File debug = new File(canonical, "debug/" 
              + input.getName() + ".dbg");
            OutputStream fout = new FileOutputStream(debug);
            fout.write(actual);
            fout.close();
            
            File expected = new File(output, input.getName() + ".out");
            assertEquals(
              input.getName(), expected.length(), actual.length);
            byte[] expectedBytes = new byte[actual.length];
            InputStream fin = new FileInputStream(expected);
            DataInputStream in = new DataInputStream(fin);
            try {
                in.readFully(expectedBytes);
            }
            finally {
                in.close();
            }
            for (int j = 0; j < expectedBytes.length; j++) {
                assertEquals(expectedBytes[i], actual[i]);   
            }
            
        }
        
    }
    
    
    public void testNamedAlgorithmWithComments() 
      throws ParsingException, IOException {
      
        File tests = input;
        String[] inputs = tests.list(new XMLFilter());
        for (int i = 0; i < inputs.length; i++) {
            File input = new File(tests, inputs[i]);   
            Document doc = builder.build(input);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                Canonicalizer serializer = new Canonicalizer(
                  out, Canonicalizer.CANONICAL_XML_WITH_COMMENTS);
                serializer.write(doc);
            }
            finally {
                out.close();
            }            
            byte[] actual = out.toByteArray();
            
            // for debugging
            File debug = new File(canonical, "debug/" 
              + input.getName() + ".dbg");
            OutputStream fout = new FileOutputStream(debug);
            fout.write(actual);
            fout.close();
            
            File expected = new File(output, input.getName() + ".out");
            assertEquals(
              input.getName(), expected.length(), actual.length);
            byte[] expectedBytes = new byte[actual.length];
            InputStream fin = new FileInputStream(expected);
            DataInputStream in = new DataInputStream(fin);
            try {
                in.readFully(expectedBytes);
            }
            finally {
                in.close();
            }
            for (int j = 0; j < expectedBytes.length; j++) {
                assertEquals(expectedBytes[i], actual[i]);   
            }
            
        }
        
    }
    
    
    public void testWithoutComments() 
      throws ParsingException, IOException {
      
        File tests = input;
        String[] inputs = tests.list(new XMLFilter());
        for (int i = 0; i < inputs.length; i++) {
            File input = new File(tests, inputs[i]); 
            Document doc = builder.build(input);
           
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                Canonicalizer serializer 
                  = new Canonicalizer(out, false);
                serializer.write(doc);
            }
            finally {
                out.close();
            }
            
            byte[] actual = out.toByteArray();
            
            File expected = new File(canonical, "wocommentsoutput/");
            expected = new File(expected, input.getName() + ".out");
            byte[] expectedBytes = new byte[actual.length];
            InputStream fin = new FileInputStream(expected);
            DataInputStream in =  new DataInputStream(fin);
            try {
                in.readFully(expectedBytes);
            }
            finally {
                in.close();
            }
            for (int j = 0; j < expectedBytes.length; j++) {
                assertEquals(expectedBytes[i], actual[i]);   
            }
            out.close();

        }
        
    }   
    
    
    public void testNamedAlgorithmWithoutComments() 
      throws ParsingException, IOException {
      
        File tests = input;
        String[] inputs = tests.list(new XMLFilter());
        for (int i = 0; i < inputs.length; i++) {
            File input = new File(tests, inputs[i]); 
            Document doc = builder.build(input);
           
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                Canonicalizer serializer = new Canonicalizer(
                  out, Canonicalizer.CANONICAL_XML);
                serializer.write(doc);
            }
            finally {
                out.close();
            }
            
            byte[] actual = out.toByteArray();
            
            File expected = new File(canonical, "wocommentsoutput/");
            expected = new File(expected, input.getName() + ".out");
            byte[] expectedBytes = new byte[actual.length];
            InputStream fin = new FileInputStream(expected);
            DataInputStream in =  new DataInputStream(fin);
            try {
                in.readFully(expectedBytes);
            }
            finally {
                in.close();
            }
            for (int j = 0; j < expectedBytes.length; j++) {
                assertEquals(expectedBytes[i], actual[i]);   
            }
            out.close();

        }
        
    }   
    
    
    public void testXMLNamespaceAttributeInheritance() 
      throws IOException {
     
        Element root = new Element("root");
        Document doc = new Document(root);
        root.addAttribute(new Attribute("xml:id", Namespace.XML_NAMESPACE, "p1"));
        root.appendChild(new Element("child312"));
        
        String expected = "<child312 xml:id=\"p1\"></child312>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out, false);
            serializer.write(doc.query("/*/child312"));
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }
    
    
    public void testXMLNamespaceAttributeNotInheritedWithExclusiveCanonicalization() 
      throws IOException {
     
        Element root = new Element("root");
        Document doc = new Document(root);
        root.addAttribute(new Attribute("xml:id", Namespace.XML_NAMESPACE, "p1"));
        root.appendChild(new Element("child312"));
        
        String expected = "<child312></child312>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out, 
              Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION);
            serializer.write(doc.query("/*/child312"));
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }
    
    
    public void testXMLNSAttributeNotInheritedWithExclusiveCanonicalization() 
      throws IOException {
     
        Element root = new Element("root", "http://www.example.org/");
        Document doc = new Document(root);
        root.appendChild(new Element("child312", "http://www.example.org/"));
        
        String expected = "<child312 xmlns=\"http://www.example.org/\"></child312>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out,
              Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION);
            XPathContext context = new XPathContext("pre", "http://www.example.org/");
            serializer.write(doc.query("/*/pre:child312 | /*/pre:child312/namespace::node()", 
             context));
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }
    
    
    public void testXMLNSPrefixAttributeInheritedWithExclusiveCanonicalization() 
      throws IOException {
     
        Element root = new Element("pre:root", "http://www.example.org/");
        Document doc = new Document(root);
        root.appendChild(new Element("pre:child312", "http://www.example.org/"));
        
        String expected = "<pre:child312 xmlns:pre=\"http://www.example.org/\"></pre:child312>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            XPathContext context = new XPathContext("pre", "http://www.example.org/");
            Canonicalizer serializer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION);
            serializer.write(doc.query("/*/pre:child312 | /*/pre:child312/namespace::node()", context));
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }
    
    
    public void testXMLNSEqualsEmptyString() 
      throws IOException {
     
        Element root = new Element("root", "http://www.ietf.org");
        Document doc = new Document(root);
        root.appendChild(new Element("child"));
        
        String expected = "<root xmlns=\"http://www.ietf.org\"><child xmlns=\"\"></child></root>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out, false);
            serializer.write(doc);
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }
    
    
    // from section 3.7 of spec
    public void testDocumentSubsetCanonicalization() 
      throws ParsingException, IOException {
        
        String input = "<!DOCTYPE doc [\n"
            + "<!ATTLIST e2 xml:space (default|preserve) 'preserve'>\n"
            + "<!ATTLIST e3 id ID #IMPLIED>\n"
            + "]>\n"
            + "<doc xmlns=\"http://www.ietf.org\" xmlns:w3c=\"http://www.w3.org\">\n"
            + "   <e1>\n"
            + "      <e2 xmlns=\"\">\n"
            + "         <e3 id=\"E3\"/>\n"
            + "      </e2>\n"
            + "   </e1>\n"
            + "</doc>";
        
        Document doc = builder.build(input, null);
        XPathContext context = new XPathContext("ietf", "http://www.ietf.org");
        String xpath = "(//. | //@* | //namespace::*)\n"
            + "[\n"
            + "self::ietf:e1 or (parent::ietf:e1 and not(self::text() or self::e2))"
            + " or\n"
            + " count(id(\"E3\")|ancestor-or-self::node()) = count(ancestor-or-self::node())\n"
            + "]";
        
        String expected = "<e1 xmlns=\"http://www.ietf.org\" xmlns:w3c=\"http://www.w3.org\"><e3 xmlns=\"\" id=\"E3\" xml:space=\"preserve\"></e3></e1>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out, false);
            serializer.write(doc.query(xpath, context));
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }
    
    
    public void testCanonicalizeEmptyDocumentSubset() 
      throws ParsingException, IOException {
        
        String input = "<!DOCTYPE doc [\n"
            + "<!ATTLIST e2 xml:space (default|preserve) 'preserve'>\n"
            + "<!ATTLIST e3 id ID #IMPLIED>\n"
            + "]>\n"
            + "<doc xmlns=\"http://www.ietf.org\" xmlns:w3c=\"http://www.w3.org\">\n"
            + "   <e1>\n"
            + "      <e2 xmlns=\"\">\n"
            + "         <e3 id=\"E3\"/>\n"
            + "      </e2>\n"
            + "   </e1>\n"
            + "</doc>";
        
        Document doc = builder.build(input, null);
        XPathContext context = new XPathContext("ietf", "http://www.ietf.org");
        String xpath = "//aaa";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out, false);
            serializer.write(doc.query(xpath, context));
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals("", actual);
        
    }
    
    
    public void testDocumentSubsetCanonicalizationSkippingProcessingInstructions() 
      throws ParsingException, IOException {
        
        String input = "<!DOCTYPE doc [\n"
            + "<!ATTLIST e3 id ID #IMPLIED>\n"
            + "]>\n<?test?>"
            + "<doc xmlns=\"http://www.ietf.org\" xmlns:w3c=\"http://www.w3.org\">\n"
            + "   <e1><?test?>\n"
            + "      <e2 xmlns=\"\">\n"
            + "         <e3 id=\"E3\"/>\n"
            + "      </e2>\n"
            + "   </e1>\n"
            + "</doc><?test?>";
        
        String expected = "<doc xmlns=\"http://www.ietf.org\" xmlns:w3c=\"http://www.w3.org\">\n"
            + "   <e1>\n"
            + "      <e2 xmlns=\"\">\n"
            + "         <e3 id=\"E3\"></e3>\n"
            + "      </e2>\n"
            + "   </e1>\n"
            + "</doc>";
        
        Document doc = builder.build(input, null);
        XPathContext context = new XPathContext("ietf", "http://www.ietf.org");
        String xpath = "//* | //text() | //@* | //namespace::*";
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out, false);
            serializer.write(doc.query(xpath, context));
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }
    
    
    public void testDocumentSubsetCanonicalizationWithoutSelectingPrologAndEpilog() 
      throws ParsingException, IOException {
        
        String input = "<!-- prolog -->\n"
            + "<!DOCTYPE doc [\n"
            + "<!ATTLIST e2 xml:space (default|preserve) 'preserve'>\n"
            + "<!ATTLIST e3 id ID #IMPLIED>\n"
            + "]>\n"
            + "<doc xmlns=\"http://www.ietf.org\" xmlns:w3c=\"http://www.w3.org\">\n"
            + "   <e1>\n"
            + "      <e2 xmlns=\"\">\n"
            + "         <e3 id=\"E3\"/>\n"
            + "      </e2>\n"
            + "   </e1>\n"
            + "</doc><!-- epilog -->";
        
        Document doc = builder.build(input, null);
        XPathContext context = new XPathContext("ietf", "http://www.ietf.org");
        String xpath = "(/*//. | //@* | //namespace::*)\n"
            + "[\n"
            + "self::ietf:e1 or (parent::ietf:e1 and not(self::text() or self::e2))"
            + " or\n"
            + " count(id(\"E3\")|ancestor-or-self::node()) = count(ancestor-or-self::node())\n"
            + "]";
        
        String expected = "<e1 xmlns=\"http://www.ietf.org\" xmlns:w3c=\"http://www.w3.org\"><e3 xmlns=\"\" id=\"E3\" xml:space=\"preserve\"></e3></e1>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out, Canonicalizer.CANONICAL_XML_WITH_COMMENTS);
            serializer.write(doc.query(xpath, context));
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }
    
    
    public void testEmptyDefaultNamespace() 
      throws ParsingException, IOException {
        
        String input = "<doc xmlns=\"http://www.ietf.org\">"
            + "<e2 xmlns=\"\"></e2>"
            + "</doc>";
        
        Document doc = builder.build(input, null);
        String xpath = "(//* | //namespace::*)";
        
        String expected = input;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out, false);
            serializer.write(doc.query(xpath));
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }

    
    
    public void testDocumentSubsetCanonicalizationSimple() 
      throws ParsingException, IOException {
        
        String input = "<!DOCTYPE doc [\n"
            + "<!ATTLIST e2 xml:space (default|preserve) 'preserve'>\n"
            + "<!ATTLIST e3 id ID #IMPLIED>\n"
            + "]>\n"
            + "<doc xmlns=\"http://www.ietf.org\" xmlns:w3c=\"http://www.w3.org\">\n"
            + "   <e1>\n"
            + "      <e2 xmlns=\"\">\n"
            + "         <e3 id=\"E3\"/>\n"
            + "      </e2>\n"
            + "   </e1>\n"
            + "</doc>";
        
        Document doc = builder.build(input, null);
        XPathContext context = new XPathContext("ietf", "http://www.ietf.org");
        String xpath = "(/* | /*/namespace::*)\n";
        
        String expected = "<doc xmlns=\"http://www.ietf.org\" xmlns:w3c=\"http://www.w3.org\"></doc>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out, false);
            serializer.write(doc.query(xpath, context));
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }
    

    public void testCanonicalizeDocumentSubsetIncludingRoot() 
      throws ParsingException, IOException {
        
        String input = "<doc />";
        
        Document doc = builder.build(input, null);
        
        String expected = "<doc></doc>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out, false);
            Nodes subset = doc.query("//.");
            serializer.write(subset);
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }
    

    public void testDocumentSubsetCanonicalizationNamespaceInheritance() 
      throws ParsingException, IOException {
        
        String input = "<!DOCTYPE doc [\n"
            + "<!ATTLIST e2 xml:space (default|preserve) 'preserve'>\n"
            + "<!ATTLIST e3 id ID #IMPLIED>\n"
            + "]>\n"
            + "<doc xmlns=\"http://www.ietf.org\" xmlns:w3c=\"http://www.w3.org\">\n"
            + "   <e1>\n"
            + "      <e2 xmlns=\"\">\n"
            + "         <e3 id=\"E3\"/>\n"
            + "      </e2>\n"
            + "   </e1>\n"
            + "</doc>";
        
        Document doc = builder.build(input, null);
        XPathContext context = new XPathContext("ietf", "http://www.ietf.org");
        String xpath = "(/*/* | /*/*/namespace::*)\n";
        
        String expected = "<e1 xmlns=\"http://www.ietf.org\" xmlns:w3c=\"http://www.w3.org\"></e1>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out, false);
            serializer.write(doc.query(xpath, context));
        }
        finally {
            out.close();
        }
            
        String actual = new String(out.toByteArray(), "UTF-8");
        assertEquals(expected, actual);
        
    }
    
    
    public void testRelativeNamespaceURIsForbidden() 
      throws ParsingException, IOException {
        
        try {
            String data = "<test xmlns=\"relative\">data</test>";
            Document doc = builder.build(data, "http://www.ex.org/");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Canonicalizer serializer
              = new Canonicalizer(out, false);
            serializer.write(doc);
            fail("Canonicalized document with relative namespace URI");
        }
        catch (ParsingException success) {
            assertNotNull(success.getMessage());
        }    
        
    }
    
    
    private static class XMLFilter implements FilenameFilter {
                
        public boolean accept(File directory, String name) {
            if (name.endsWith(".xml")) return true;
            return false;           
        }
        
    }
    
    
    public void testNFCFromISO88591() 
      throws ParsingException, IOException {
        isoNormalizationTest("ISO-8859-1");
    }
    
    
    public void testNFCFromISO88592() 
      throws ParsingException, IOException {
        isoNormalizationTest("ISO-8859-2");
    }
    
    
    public void testNFCFromISO88593() 
      throws ParsingException, IOException {
        isoNormalizationTest("ISO-8859-3");
    }
    
    
    public void testNFCFromISO88594() 
      throws ParsingException, IOException {
        isoNormalizationTest("ISO-8859-4");
    }
    
    
    public void testNFCFromISO88595() 
      throws ParsingException, IOException {
        isoNormalizationTest("ISO-8859-5");
    }
    
    
    public void testNFCFromISO88596() 
      throws ParsingException, IOException {
        
        // This test fails in 1.2.2 due to an apparent bug in the 
        // conversion of the characters '1' and '0' to bytes in 
        // ISO-8859-6
        isoNormalizationTest("ISO-8859-6");
        
    }
    
    
    public void testNFCFromISO88597() 
      throws ParsingException, IOException {
        isoNormalizationTest("ISO-8859-7");
    }
    
    
    public void testNFCFromISO88598() 
      throws ParsingException, IOException {
        isoNormalizationTest("ISO-8859-8");
    }
    
    
    public void testNFCFromISO88599() 
      throws ParsingException, IOException {
        isoNormalizationTest("ISO-8859-9");
    }
    
    
    public void testNFCFromISO885913() 
      throws ParsingException, IOException {
        
        if (version >= 1.3) {
            // iSO-8859-6 not supported in Java 1.2
            isoNormalizationTest("ISO-8859-13");
        }
        
    }

    
    public void testNFCFromISO885915() 
      throws ParsingException, IOException {
        isoNormalizationTest("ISO-8859-15");
    }
    
    
    // 14 and 16 aren't tested because Java doesn't support them yet
    private void isoNormalizationTest(String encoding)
      throws ParsingException, IOException {
        
        String prolog = "<?xml version='1.0' encoding='" 
          + encoding + "'?>\r\n<root>";
        
        byte[] prologData = prolog.getBytes(encoding);
        
        String epilog = "</root>";
        byte[] epilogData = epilog.getBytes(encoding);      
        byte[] data = new byte[prologData.length + epilogData.length + 255 - 160 + 1];
        System.arraycopy(prologData, 0, data, 0, prologData.length);
        System.arraycopy(epilogData, 0, data, 
          data.length - epilogData.length, epilogData.length);
        for (int i = 160; i <= 255; i++) {
            data[prologData.length + (i-160)] = (byte) i;   
        }
        InputStream in = new ByteArrayInputStream(data);
        Document doc = builder.build(in);
        String rawResult = doc.getValue();
        String normalizedResult = Normalizer.normalize(rawResult, Normalizer.NFC);
        assertEquals("Parser doesn't use NFC when converting from " + encoding, 
          normalizedResult, rawResult);
        
    }

    
    public void testEBCDIC()
      throws ParsingException, IOException {
          
        String encoding = "IBM037";
        String prolog = "<?xml version='1.0' encoding='" 
          + encoding + "'?>\r\n<root>";
        byte[] prologData = prolog.getBytes(encoding);
        String epilog = "</root>";
        byte[] epilogData = epilog.getBytes(encoding);      
        byte[] data = new byte[prologData.length + epilogData.length + 255 - 160 + 1];
        System.arraycopy(prologData, 0, data, 0, prologData.length);
        System.arraycopy(epilogData, 0, data, 
          data.length - epilogData.length, epilogData.length);
        StringBuffer buffer = new StringBuffer(255 - 160 + 1);
        for (int i = 160; i <= 255; i++) {
            buffer.append((char) i);   
        }
        byte[] temp = buffer.toString().getBytes(encoding);
        System.arraycopy(temp, 0, data, prologData.length, temp.length);        
        InputStream in = new ByteArrayInputStream(data);
        Document doc = builder.build(in);
        String rawResult = doc.getValue();
        String normalizedResult = Normalizer.normalize(rawResult, Normalizer.NFC);
        assertEquals("Parser doesn't use NFC when converting from " + encoding, 
          normalizedResult, rawResult);
        
    }

    
    // make sure null pointer exception doesn't cause any output
    public void testNullDocument() 
      throws IOException {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out);
        try {
            canonicalizer.write((Document) null);  
            fail("Wrote null document"); 
        }   
        catch (NullPointerException success) {
            // success   
        }
        byte[] result = out.toByteArray();
        assertEquals(0, result.length);
        
    }
    
    
    public void testWhiteSpaceTrimmingInNonCDATAAttribute() 
      throws IOException {
        
        Attribute attribute = new Attribute("name", "  value1  value2  ");
        attribute.setType(Attribute.Type.NMTOKENS);
        Element root = new Element("root");
        root.addAttribute(attribute);
        Document doc = new Document(root);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out);
        canonicalizer.write(doc);
        out.close();
        String result = new String(out.toByteArray(), "UTF8");
        assertEquals("<root name=\"value1 value2\"></root>", result);
        
    }
    
    
    // compare to output generated by Apache XML Security code
    public void testXMLConformanceTestSuiteDocuments() 
      throws ParsingException, IOException {
      
        File masterList = new File(canonical, "xmlconf");
        masterList = new File(masterList, "xmlconf.xml");
        if (masterList.exists()) {
            Document xmlconf = builder.build(masterList);
            Elements testcases = xmlconf.getRootElement().getChildElements("TESTCASES");
            processTestCases(testcases);
        }

    }

    
    // xmlconf/xmltest/valid/sa/097.xml appears to be screwed up by a lot
    // of parsers 
    private void processTestCases(Elements testcases) 
      throws ParsingException, IOException {
        
        for (int i = 0; i < testcases.size(); i++) {
              Element testcase = testcases.get(i); 
              Elements tests = testcase.getChildElements("TEST");
              processTests(tests);
              Elements level2 = testcase.getChildElements("TESTCASES");
              // need to be recursive to handle recursive IBM test cases
              processTestCases(level2);
        }
        
    }


    private void processTests(Elements tests) 
      throws ParsingException, IOException  {
        
        for (int i = 0; i < tests.size(); i++) {
            Element test = tests.get(i);
            String namespace = test.getAttributeValue("NAMESPACE");
            if ("no".equals(namespace)) continue;
            String type = test.getAttributeValue("TYPE");
            if ("not-wf".equals(type)) continue;
            String uri = test.getAttributeValue("URI");
            String base = test.getBaseURI();
            // Hack because URIUtil isn't public; and I don't want to
            // depend on 1.4 only java.net.URI
            Element parent = new Element("e");
            parent.setBaseURI(base);
            Element child = new Element("a");
            child.addAttribute(new Attribute("xml:base", 
              "http://www.w3.org/XML/1998/namespace", uri));
            parent.appendChild(child);
            String resolvedURI = child.getBaseURI();
            
            Document doc = builder.build(resolvedURI);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                Canonicalizer serializer = new Canonicalizer(out);
                serializer.write(doc);
            }
            finally {
                out.close();
            }           
            byte[] actual = out.toByteArray();
            
            File input = new File(resolvedURI.substring(5) + ".can");
            assertEquals(resolvedURI, input.length(), actual.length);
            byte[] expected = new byte[actual.length];
            DataInputStream in = new DataInputStream(
              new BufferedInputStream(new FileInputStream(input)));
            try {
                in.readFully(expected);
            }
            finally {
                in.close();
            }
            for (int j = 0; j < expected.length; j++) {
                assertEquals(resolvedURI + " at byte " + j,
                  expected[j], actual[j]);
            }
            
        }
        
    }

    
    public void testExclusive() throws IOException {
     
        Element pdu = new Element("n0:pdu", "http://a.example");
        Element elem1 = new Element("n1:elem1", "http://b.example");
        elem1.appendChild("content");
        pdu.appendChild(elem1);
        
        String expected = "<n1:elem1 xmlns:n1=\"http://b.example\">content</n1:elem1>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        XPathContext context = new XPathContext("n1", "http://b.example");
        Document doc = new Document(pdu);
        canonicalizer.write(doc.query("(//. | //@* | //namespace::*)[ancestor-or-self::n1:elem1]", context));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
    
    
    public void testExclusiveWithNamespacedAttributes() throws IOException {
     
        Element pdu = new Element("n0:pdu", "http://a.example");
        Element elem1 = new Element("n1:elem1", "http://b.example");
        elem1.appendChild("content");
        pdu.appendChild(elem1);
        elem1.addAttribute(new Attribute("pre:foo", "http://www.example.org/", "value"));
        
        String expected = "<n1:elem1 xmlns:n1=\"http://b.example\" xmlns:pre=\"http://www.example.org/\" pre:foo=\"value\">content</n1:elem1>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        XPathContext context = new XPathContext("n1", "http://b.example");
        Document doc = new Document(pdu);
        canonicalizer.write(doc.query("(//. | //@* | //namespace::*)[ancestor-or-self::n1:elem1]", context));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
    

    /* <root xml:lang="en"><a><b>test</b></a></root>

Choose the document subset selected by /root//node()

and expect to see

<a xml:lang="en"><b>test</b></a> */
    public void testInheritanceOfXMLLang() throws IOException {
     
        Element root = new Element("root");
        root.addAttribute(new Attribute("xml:lang", Namespace.XML_NAMESPACE, "en"));
        Element a = new Element("a");
        Element b = new Element("b");
        b.appendChild("test");
        a.appendChild(b);
        root.appendChild(a);
        
        String expected = "<a xml:lang=\"en\"><b>test</b></a>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out, Canonicalizer.CANONICAL_XML);
        
        Document doc = new Document(root);
        canonicalizer.write(doc.query("/root//node()"));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }    
        

    public void testExclusiveWithInclusiveNamespaces() 
      throws IOException {
     
        Element pdu = new Element("n0:pdu", "http://a.example");
        Element elem1 = new Element("n1:elem1", "http://b.example");
        elem1.appendChild("content");
        pdu.appendChild(elem1);
        
        String expected = "<n1:elem1 xmlns:n0=\"http://a.example\""
          + " xmlns:n1=\"http://b.example\">content</n1:elem1>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        XPathContext context = new XPathContext("n1", "http://b.example");
        Document doc = new Document(pdu);
        canonicalizer.setInclusiveNamespacePrefixList("n0");
        Nodes subset = doc.query(
          "(//. | //@* | //namespace::*)[ancestor-or-self::n1:elem1]",
          context);
        canonicalizer.write(subset);  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }


    public void testClearInclusiveNamespacePrefixes() 
      throws IOException {
     
        Element pdu = new Element("n0:pdu", "http://a.example");
        Element elem1 = new Element("n1:elem1", "http://b.example");
        elem1.appendChild("content");
        pdu.appendChild(elem1);
        
        String expected = "<n1:elem1"
          + " xmlns:n1=\"http://b.example\">content</n1:elem1>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        XPathContext context = new XPathContext("n1", "http://b.example");
        Document doc = new Document(pdu);
        canonicalizer.setInclusiveNamespacePrefixList("n0");
        canonicalizer.setInclusiveNamespacePrefixList(null);
        Nodes subset = doc.query(
          "(//. | //@* | //namespace::*)[ancestor-or-self::n1:elem1]",
          context);
        canonicalizer.write(subset);  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }


    public void testExclusive22a() throws ParsingException, IOException {
     
        Builder builder = new Builder();
        String input = "<n0:local xmlns:n0='foo:bar' xmlns:n3='ftp://example.org'>" +
                "<n1:elem2 xmlns:n1=\"http://example.net\" xml:lang=\"en\">"
            + "<n3:stuff xmlns:n3=\"ftp://example.org\"/></n1:elem2></n0:local>";
        Document doc = builder.build(input, null);
        
        String expected = "<n1:elem2 xmlns:n1=\"http://example.net\" xml:lang=\"en\">" +
                "<n3:stuff xmlns:n3=\"ftp://example.org\"></n3:stuff></n1:elem2>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        XPathContext context = new XPathContext("n1", "http://example.net");
        canonicalizer.write(doc.query(" (//. | //@* | //namespace::*)[ancestor-or-self::n1:elem2]", context));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
        

    public void testExclusive22b() throws ParsingException, IOException {
     
        Builder builder = new Builder();
        String input = "<n2:pdu xmlns:n1='http://example.com' "
            + "xmlns:n2='http://foo.example' xml:lang='fr' xml:space='retain'>"
            + "<n1:elem2 xmlns:n1='http://example.net' xml:lang='en'>"
            + "<n3:stuff xmlns:n3='ftp://example.org'/></n1:elem2></n2:pdu>";
        Document doc = builder.build(input, null);
        
        String expected = "<n1:elem2 xmlns:n1=\"http://example.net\" xml:lang=\"en\">"
            + "<n3:stuff xmlns:n3=\"ftp://example.org\"></n3:stuff></n1:elem2>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Canonicalizer canonicalizer = new Canonicalizer(out,
          Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS);
        
        XPathContext context = new XPathContext("n1", "http://example.net");
        canonicalizer.write(doc.query(" (//. | //@* | //namespace::*)[ancestor-or-self::n1:elem2]", context));  
        
        byte[] result = out.toByteArray();
        out.close();
        String s = new String(out.toByteArray(), "UTF8");
        assertEquals(expected, s);
        
    }
        
    
    // compare to output generated by Apache XML Security code
    public void testExclusiveXMLConformanceTestSuiteDocuments() 
      throws ParsingException, IOException {
      
        File masterList = new File(canonical, "xmlconf");
        masterList = new File(masterList, "xmlconf.xml");
        if (masterList.exists()) {
            Document xmlconf = builder.build(masterList);
            Elements testcases = xmlconf.getRootElement().getChildElements("TESTCASES");
            processExclusiveTestCases(testcases);
        }

    }

    
    // xmlconf/xmltest/valid/sa/097.xml appears to be screwed up by a lot
    // of parsers 
    private void processExclusiveTestCases(Elements testcases) 
      throws ParsingException, IOException {
        
        for (int i = 0; i < testcases.size(); i++) {
              Element testcase = testcases.get(i); 
              Elements tests = testcase.getChildElements("TEST");
              processExclusiveTests(tests);
              Elements level2 = testcase.getChildElements("TESTCASES");
              // need to be recursive to handle recursive IBM test cases
              processExclusiveTestCases(level2);
        }
        
    }


    private void processExclusiveTests(Elements tests) 
      throws ParsingException, IOException  {
        
        for (int i = 0; i < tests.size(); i++) {
            Element test = tests.get(i);
            String namespace = test.getAttributeValue("NAMESPACE");
            if ("no".equals(namespace)) continue;
            String type = test.getAttributeValue("TYPE");
            if ("not-wf".equals(type)) continue;
            String uri = test.getAttributeValue("URI");
            String base = test.getBaseURI();
            // Hack because URIUtil isn't public; and I don't want to
            // depend on 1.4 only java.net.URI
            Element parent = new Element("e");
            parent.setBaseURI(base);
            Element child = new Element("a");
            child.addAttribute(new Attribute("xml:base", 
              "http://www.w3.org/XML/1998/namespace", uri));
            parent.appendChild(child);
            String resolvedURI = child.getBaseURI();
            
            Document doc = builder.build(resolvedURI);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                Canonicalizer serializer = new Canonicalizer(
                  out, 
                  Canonicalizer.EXCLUSIVE_XML_CANONICALIZATION_WITH_COMMENTS
                );
                serializer.write(doc);
            }
            finally {
                out.close();
            }           
            byte[] actual = out.toByteArray();
            
            File input = new File(resolvedURI.substring(5) + ".exc");
            byte[] expected = new byte[(int) input.length()];
            DataInputStream in = new DataInputStream(
              new BufferedInputStream(new FileInputStream(input)));
            try {
                in.readFully(expected);
            }
            finally {
                in.close();
            }
            
            assertEquals(resolvedURI, new String(expected, "UTF-8"), new String(actual, "UTF-8"));
            
        }
        
    }
    
    
    public void testCanonicalizeAttribute() throws IOException {
     
        Attribute att = new Attribute("pre:foo", "http://www.example.org", "value");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out);
            serializer.write(att);
        }
        finally {
            out.close();
        }           
        byte[] actual = out.toByteArray();
        byte[] expected = " pre:foo=\"value\"".getBytes("UTF-8");
        assertEquals(expected, actual);
        
    }

    
    public void testCanonicalizeNamespace() throws IOException {
     
        Element element = new Element("pre:foo", "http://www.example.org");
        Nodes namespaces = element.query("namespace::pre");
        Namespace ns = (Namespace) namespaces.get(0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out);
            serializer.write(ns);
        }
        finally {
            out.close();
        }           
        byte[] actual = out.toByteArray();
        byte[] expected = " xmlns:pre=\"http://www.example.org\"".getBytes("UTF-8");
        assertEquals(expected, actual);
        
        
    }

    
    public void testCanonicalizeDefaultNamespace() throws IOException {
     
        Element element = new Element("foo", "http://www.example.org");
        Nodes namespaces = element.query("namespace::*");
        Namespace ns = (Namespace) namespaces.get(0);
        if (ns.getPrefix().equals("xml")) ns = (Namespace) namespaces.get(1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out);
            serializer.write(ns);
        }
        finally {
            out.close();
        }           
        byte[] actual = out.toByteArray();
        byte[] expected = " xmlns=\"http://www.example.org\"".getBytes("UTF-8");
        assertEquals(expected, actual);  
        
    }

    
    public void testCanonicalizeXMLNamespace() throws IOException {
     
        Element element = new Element("foo");
        Nodes namespaces = element.query("namespace::*");
        Namespace ns = (Namespace) namespaces.get(0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out);
            serializer.write(ns);
        }
        finally {
            out.close();
        }           
        byte[] actual = out.toByteArray();
        byte[] expected = " xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"".getBytes("UTF-8");
        assertEquals(expected, actual);  
        
    }

    
    public void testCanonicalizeElement() throws IOException {
     
        Element element = new Element("pre:foo", "http://www.example.org");
        element.appendChild("  value \n value");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out);
            serializer.write(element);
        }
        finally {
            out.close();
        }           
        byte[] actual = out.toByteArray();
        byte[] expected = "<pre:foo xmlns:pre=\"http://www.example.org\">  value \n value</pre:foo>".getBytes("UTF-8");
        assertEquals(expected, actual);
        
    }

    
    public void testCanonicalizeDocumentTypeDeclaration() throws IOException {
     
        DocType doctype = new DocType("root", "http://www.example.org");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out);
            serializer.write(doctype);
        }
        finally {
            out.close();
        }           
        byte[] actual = out.toByteArray();
        assertEquals(0, actual.length);
        
    }

    
    public void testCanonicalizeProcessingInstruction() throws IOException {
     
        ProcessingInstruction pi = new ProcessingInstruction("target", "value \n value");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out);
            serializer.write(pi);
        }
        finally {
            out.close();
        }           
        byte[] actual = out.toByteArray();
        byte[] expected = "<?target value \n value?>".getBytes("UTF-8");
        assertEquals(expected, actual);
        
    }

    
    public void testCanonicalizeText() throws IOException {
     
        Text c = new Text("  pre:foo \n  ");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out);
            serializer.write(c);
        }
        finally {
            out.close();
        }           
        byte[] actual = out.toByteArray();  
        byte[] expected = "  pre:foo \n  ".getBytes("UTF-8");
        assertEquals(expected, actual);        
        
        
    }

    
    public void testCanonicalizeComment() throws IOException {
     
        Comment c = new Comment("pre:foo");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Canonicalizer serializer = new Canonicalizer(out);
            serializer.write(c);
        }
        finally {
            out.close();
        }           
        byte[] actual = out.toByteArray();
        byte[] expected = "<!--pre:foo-->".getBytes("UTF-8");
        assertEquals(expected, actual);
        
    }
    
    
    public void testUnsupportedAlgorithm() throws IOException {
     
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new Canonicalizer(out, "http://www.example.org/canonical");
            fail("Allowed unrecognized algorithm");
        }
        catch (CanonicalizationException success) {
            assertNotNull(success.getMessage());
        } 
        finally {
            out.close();
        }
        
    }
    
    
    public void testCanonicalizeDetachedNodes() throws IOException {
     
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Element e = new Element("test");
        Nodes nodes = new Nodes(e);
        Canonicalizer serializer = new Canonicalizer(out);
        try {
            serializer.write(nodes);
            fail("Canonicalized detached node");
        }
        catch (CanonicalizationException success) {
            assertNotNull(success.getMessage());
        } 
        finally {
            out.close();
        }
        
    }
    
    
    public void testCanonicalizeNodesFromTwoDocuments() throws IOException {
     
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Element e1 = new Element("test");
        Document d1 = new Document(e1);
        Element e2 = new Element("test");
        Document d2 = new Document(e2);
        Nodes nodes = new Nodes(e1);
        nodes.append(e2);
        Canonicalizer serializer = new Canonicalizer(out);
        try {
            serializer.write(nodes);
            fail("Canonicalized multiple document nodes");
        }
        catch (CanonicalizationException success) {
            assertNotNull(success.getMessage());
        } 
        finally {
            out.close();
        }
        
    }
    
    
    // ???? pull up into XOMTestCase?
    public void assertEquals(byte[] expected, byte[] actual) {
        
        if (expected == null && actual == null) {
            return;
        }
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
        
    }

    
}
