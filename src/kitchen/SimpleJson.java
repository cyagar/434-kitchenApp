package kitchen;

import java.util.*;

/** Small dependency-free JSON reader for API responses. */
final class SimpleJson {
    private final String text;
    private int pos;
    private SimpleJson(String text) { this.text = text; }
    static Object parse(String text) {
        SimpleJson p = new SimpleJson(text);
        Object result = p.value();
        p.ws();
        if (p.pos != text.length()) throw new IllegalArgumentException("Invalid JSON ending");
        return result;
    }
    private void ws() { while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++; }
    private char peek() { ws(); return pos < text.length() ? text.charAt(pos) : '\0'; }
    private char take() { if(pos>=text.length()) throw new IllegalArgumentException("Unexpected end of JSON"); return text.charAt(pos++); }
    private void expect(char c) { ws(); if (take()!=c) throw new IllegalArgumentException("Invalid JSON: expected " + c); }
    private Object value() {
        char c=peek();
        if(c=='"') return string();
        if(c=='{') return object();
        if(c=='[') return array();
        if(c=='t' && text.startsWith("true",pos)){pos+=4;return true;}
        if(c=='f' && text.startsWith("false",pos)){pos+=5;return false;}
        if(c=='n' && text.startsWith("null",pos)){pos+=4;return null;}
        if(c=='-' || Character.isDigit(c)) return number();
        throw new IllegalArgumentException("Invalid JSON value at " + pos);
    }
    private String string() {
        expect('"'); StringBuilder out=new StringBuilder();
        while(true) {
            char c=take(); if(c=='"') return out.toString();
            if(c!='\\'){out.append(c);continue;}
            char e=take();
            switch(e){
                case '"':out.append('"');break;case '\\':out.append('\\');break;
                case '/':out.append('/');break;case 'b':out.append('\b');break;
                case 'f':out.append('\f');break;case 'n':out.append('\n');break;
                case 'r':out.append('\r');break;case 't':out.append('\t');break;
                case 'u':
                    if(pos+4>text.length()) throw new IllegalArgumentException("Invalid unicode escape");
                    out.append((char)Integer.parseInt(text.substring(pos,pos+4),16));pos+=4;break;
                default:throw new IllegalArgumentException("Invalid JSON escape");
            }
        }
    }
    private Number number(){int start=pos;while(pos<text.length() && "-+0123456789.eE".indexOf(text.charAt(pos))>=0)pos++;
        return Double.valueOf(text.substring(start,pos));}
    private Map<String,Object> object(){expect('{'); Map<String,Object> map=new LinkedHashMap<>();
        if(peek()=='}'){pos++;return map;}
        do{String key=string();expect(':');map.put(key,value());if(peek()=='}'){pos++;return map;}expect(',');}while(true);
    }
    private List<Object> array(){expect('[');List<Object> arr=new ArrayList<>();if(peek()==']'){pos++;return arr;}
        do{arr.add(value());if(peek()==']'){pos++;return arr;}expect(',');}while(true);
    }
    @SuppressWarnings("unchecked") static Map<String,Object> obj(Object o){return o instanceof Map?(Map<String,Object>)o:Collections.emptyMap();}
    @SuppressWarnings("unchecked") static List<Object> arr(Object o){return o instanceof List?(List<Object>)o:Collections.emptyList();}
    static String str(Object o){return o==null?"":o.toString();}
    static int num(Object o){return o instanceof Number?((Number)o).intValue():0;}
}
