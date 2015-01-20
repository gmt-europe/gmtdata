// Taken from http://stackoverflow.com/questions/4915422/get-line-number-from-xml-node-java.

package nl.gmt.data.schema;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

class PositionalDocumentLoader {
    public final static String LINE_NUMBER_KEY_NAME = "LINE_NUMBER";
    public final static String COLUMN_NUMBER_KEY_NAME = "COLUMN_NUMBER";

    public static Document loadDocument(final InputStream is) throws IOException, SAXException {
        final Document doc;
        SAXParser parser;
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (final ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        final Stack<Element> elementStack = new Stack<>();
        final StringBuilder textBuffer = new StringBuilder();
        final DefaultHandler handler = new DefaultHandler() {
            private Locator locator;

            @Override
            public void setDocumentLocator(final Locator locator) {
                this.locator = locator;
            }

            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
                addTextIfNeeded();
                final Element el = doc.createElement(qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    el.setAttribute(attributes.getQName(i), attributes.getValue(i));
                }

                el.setUserData(LINE_NUMBER_KEY_NAME, locator.getLineNumber(), null);
                el.setUserData(COLUMN_NUMBER_KEY_NAME, locator.getColumnNumber(), null);

                elementStack.push(el);
            }

            @Override
            public void endElement(final String uri, final String localName, final String qName) {
                addTextIfNeeded();
                final Element closedEl = elementStack.pop();

                // Is this the root element?
                if (elementStack.isEmpty()) {
                    doc.appendChild(closedEl);
                } else {
                    final Element parentEl = elementStack.peek();
                    parentEl.appendChild(closedEl);
                }
            }

            @Override
            public void characters(final char ch[], final int start, final int length) throws SAXException {
                textBuffer.append(ch, start, length);
            }

            @Override
            public void processingInstruction(String target, String data) throws SAXException {
                ProcessingInstruction processingInstruction = doc.createProcessingInstruction(target, data);

                // Is this the root element?
                if (elementStack.isEmpty()) {
                    doc.appendChild(processingInstruction);
                } else {
                    final Element parentEl = elementStack.peek();
                    parentEl.appendChild(processingInstruction);
                }
            }

            // Outputs text accumulated under the current node
            private void addTextIfNeeded() {
                if (textBuffer.length() > 0) {
                    final Element el = elementStack.peek();
                    final Node textNode = doc.createTextNode(textBuffer.toString());
                    el.appendChild(textNode);
                    textBuffer.delete(0, textBuffer.length());
                }
            }
        };

        parser.parse(is, handler);

        return doc;
    }
}
