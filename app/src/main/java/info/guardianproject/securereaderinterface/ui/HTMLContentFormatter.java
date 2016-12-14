package info.guardianproject.securereaderinterface.ui;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.View;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import info.guardianproject.securereader.HTMLToPlainTextFormatter;

/**
 * Created by N-Pex on 16-04-27.
 */
public class HTMLContentFormatter extends HTMLToPlainTextFormatter {

    @Override
    public CharSequence getPlainText(Element element) {
        FormattingVisitor formatter = new FormattingVisitor();
        NodeTraversor traversor = new NodeTraversor(formatter);
        traversor.traverse(element); // walk the DOM, and call .head() and .tail() for each node
        return formatter.getContent();
    }

    // the formatting rules, implemented in a breadth-first DOM traverse
    private class FormattingVisitor implements NodeVisitor {
        private SpannableStringBuilder accum = new SpannableStringBuilder(); // holds the accumulated text
        private int startOfLink = -1;
        private String ignoreUntilElement;
        private int ignoreUntilDepth;

        // hit when the node is first seen
        public void head(Node node, int depth) {
            String name = node.nodeName();
            if (ignoreUntilElement != null)
                return;
            if (node instanceof TextNode) {
                String text = ((TextNode) node).text();
                // Replace LINE SEPARATOR char
                text = text.replace("\u2028", "\r\n");
                // Replace PARAGRAPH SEPARATOR char
                text = text.replace("\u2029", "\r\n\r\n");
                append(text); // TextNodes carry all user-readable text in the DOM.
            }
            else if (name.equals("li"))
                append("\n * ");
            else if (name.equals("dt"))
                append("  ");
            else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr"))
                append("\n");
            else if (name.equals("a"))
                startOfLink = accum.length();
            else if (name.equals("div") && node instanceof Element) {
                Element element = (Element) node;
                if (element.hasAttr("class") && element.attr("class").contains("wp-caption")) {
                    ignoreUntilElement = "div";
                    ignoreUntilDepth = depth;
                }
            }
        }

        // hit when all of the node's children (if any) have been visited
        public void tail(Node node, int depth) {
            String name = node.nodeName();
            if (ignoreUntilElement != null && name != null && name.contentEquals(ignoreUntilElement) && depth == ignoreUntilDepth) {
                  ignoreUntilElement = null;
            } else if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5"))
                append("\n");
            else if (name.equals("a")) {
                if (startOfLink != -1) {
                    //Uncomment this to get clickable links
                    HTMLLinkSpan span = new HTMLLinkSpan(node.absUrl("href"));
                    accum.setSpan(span, startOfLink, accum.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    //String url = span.getURL();
                    //append(" <");
                    //append(url);
                    //span = new HTMLLinkSpan(span.getURL());
                    //accum.setSpan(span, accum.length() - url.length(), accum.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    //append(">");
                    startOfLink = -1;
                }
            }
        }

        // appends text to the string builder with a simple word wrap method
        private void append(CharSequence text) {
            accum.append(text);
        }

        public CharSequence getContent() {
            return accum;
        }
    }

    public static class HTMLLinkSpan extends ClickableSpan {
        public interface LinkListener {
            void onLinkClicked(String url);
        }
        private LinkListener listener;
        private String url;
        public HTMLLinkSpan(String url) {
            this.url = url;
        }

        @Override
        public void onClick(View widget) {
            if (listener != null)
                listener.onLinkClicked(this.url);
        }

        public void setListener(LinkListener listener) {
            this.listener = listener;
        }

        public String getURL() {
            return this.url;
        }
    }
}
