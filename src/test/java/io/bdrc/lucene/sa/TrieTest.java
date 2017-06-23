/*******************************************************************************
 * Copyright (c) 2017 Buddhist Digital Resource Center (BDRC)
 * 
 * If this file is a derivation of another work the license header will appear 
 * below; otherwise, this work is licensed under the Apache License, Version 2.0 
 * (the "License"); you may not use this file except in compliance with the 
 * License.
 * 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package io.bdrc.lucene.sa;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.bdrc.lucene.stemmer.Optimizer;
import io.bdrc.lucene.stemmer.Row;
import io.bdrc.lucene.stemmer.Trie;

import static org.hamcrest.CoreMatchers.*;

/**
 * Unit tests for the Tibetan tokenizers and filters.
 */
public class TrieTest
{
	static TokenStream tokenize(Reader reader, Tokenizer tokenizer) throws IOException {
		tokenizer.close();
		tokenizer.end();
		tokenizer.setReader(reader);
		tokenizer.reset();
		return tokenizer;
	}
		
	static private void assertTokenStream(TokenStream tokenStream, List<String> expected) {
		try {
			List<String> termList = new ArrayList<String>();
			CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
			while (tokenStream.incrementToken()) {
				termList.add(charTermAttribute.toString());
			}
			System.out.println(String.join(" ", termList));
			assertThat(termList, is(expected));
		} catch (IOException e) {
			assertTrue(false);
		}
	}

	@BeforeClass
	public static void init() {
		System.out.println("before the test sequence");
	}

	@Test
	public void sylEndTest() throws IOException
	{
		System.out.println("Testing isSylEnd()");
		SskrtSyllableTokenizer test = new SskrtSyllableTokenizer();
		char V = 'a';
		char X = 'M';
		char C = 'k';
		if (test.isSylEnd(V, C)) {
			System.out.println("V C: The end of a syllable is reached.");
		} else if (!test.isSylEnd(V, X)) {
			System.out.println("V X:  ");
		}
		test.close();
	}
	
	@Test
	public void sylTokenizerTest() throws IOException
	{
		System.out.println("Testing TibSyllableTokenizer()");
		String input = "བཀྲ་ཤིས། བདེ་ལེགས།";
		Reader reader = new StringReader(input);
		List<String> expected = Arrays.asList("བཀྲ", "ཤིས", "བདེ" ,"ལེགས");

		System.out.print(input + " => ");
		TokenStream res = tokenize(reader, new SskrtSyllableTokenizer());
		assertTokenStream(res, expected);
	}
	
	public boolean isTibLetter(int c) {
		return ('\u0F40' <= c && c <= '\u0FBC');
	}

	/**
	 *  this function is inspired from getLastOnPath() in stemmer's Trie.java
	 * @param toAnalyze the string to analyse
	 * @param startCharIndex the index from which we want to analyze
	 * @param t the Trie containing the data
	 */
	// 
	public void produceOneToken(String toAnalyze, int startCharIndex, Trie t) {
		// getting the root of the tree
//		System.out.println(toAnalyze);
		Row now = t.getRow(t.getRoot());
		int w; // temporary index variable
		int lastCharIndex = -1; // the index of the last match in the string we analyze
//		int lastCmdIndex = -1; // the index (inside the Trie) of the cmd corresponding to the last match
		
		int i = startCharIndex; // the current index in the string
		while (i < toAnalyze.length()) {
			Character ch = toAnalyze.charAt(i); // get the current character
//			System.out.println("moving to index "+i+": "+ch);
			w = now.getCmd(ch); // get the command associated with the current character at next step in the Trie
			if (w >= 0) {
				if (i >= toAnalyze.length()-1 || !isTibLetter(toAnalyze.charAt(i+1))) {
//						System.out.println("current row has an command for it, so it's a match");
//						lastCmdIndex = w;
						lastCharIndex = i;
					}
            } else {
//            	System.out.println("current row does not have a command for it, no match");
            }
			w = now.getRef(ch); // get the next row if there is one
			if (w >= 0) {
//				System.out.println("current row does have a reference for this char, further matches are possible, moving one row forward in the Trie");
                now = t.getRow(w);
            } else {
//            	System.out.println("current row does not have a reference to this char, so there's no further possible match, breaking the loop");
                break; // no more steps possible in our research
            }
			i++;
		}
		//w = now.getCmd(toAnalyze.charAt(i));
		if (lastCharIndex == -1) {
//			System.out.println("I have found nothing");
			return;
		}
//		System.out.println("I have found a token that goes from "+startCharIndex+" to "
//				+ lastCharIndex);
//		System.out.println("the substring is: "+toAnalyze.substring(startCharIndex, lastCharIndex+1));
//		System.out.println("the command associated with this token in the Trie is: "+t.getCommandVal(lastCmdIndex));
	}
	
	@Test
	public void produceOneTokenTest() throws IOException
	{
		System.out.println("Testing Stemmer Trie (produceOneToken() )");
		Trie test = new Trie(true);
		test.add("དྲོའི",">a");
		test.add("བདེ་ལེགས","=");
		test.add("བདེ", "=");
		test.add("བཀྲ་ཤིས","=");
		test.add("བཀྲ", "=");
		test.add("དྲོ","=");
		test.add("དགའི", ">A");
		test.add("དགའ","=");
		Optimizer opt = new Optimizer();
		test.reduce(opt);
		produceOneToken("དག", 0, test);
		produceOneToken("དགའི", 0, test);
		produceOneToken("བཀྲ་", 0, test);
		produceOneToken("བཀྲད", 0, test);
		produceOneToken("བདེ་ལེགས", 0, test);
	}
	@AfterClass
	public static void finish() {
		System.out.println("after the test sequence");
	}
}