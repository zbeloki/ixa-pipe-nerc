/*
 *Copyright 2014 Rodrigo Agerri

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package ixa.pipe.nerc;
import java.util.ArrayList;
import java.util.List;


import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.util.Span;

 /**
 * Named Entity Recognition module based on {@link Gazetteer} objects 
 *  
 * @author ragerri 2014/03/14
 * 
 */

 public class GazetteerNameFinder implements NameFinder {

   private static final String DEFAULT_TYPE = "MISC";
   private final String type;
   
   private NameFactory nameFactory;
   private Gazetteer gazetteer;
   private final static boolean DEBUG = true;

 
  public GazetteerNameFinder(Gazetteer gazetteer, String type) {
    this.gazetteer = gazetteer;
    if (type == null) {
      throw new IllegalArgumentException("type cannot be null!");
    }
    this.type = type;
  }
  
  public GazetteerNameFinder(Gazetteer gazetteer) {
    this(gazetteer, DEFAULT_TYPE);
  }
  
  public GazetteerNameFinder(Gazetteer gazetteer, String type, NameFactory nameFactory) {
    this.gazetteer = gazetteer;
    this.nameFactory = nameFactory;
    if (type == null) {
      throw new IllegalArgumentException("type cannot be null!");
    }
    
    this.type = type;
  }
  
  public GazetteerNameFinder(Gazetteer gazetteer, NameFactory nameFactory) {
    this(gazetteer,DEFAULT_TYPE,nameFactory);
  }
  
  /**
   * Gazetteer based Named Entity Detector. Note that this method
   * does not classify the named entities, only assigns a "MISC" tag to every
   * Named Entity. Pass the result of this function to {@link gazetteerPostProcessing} for
   * gazetteer-based Named Entity classification.
   * 
   * @param tokens
   * @param dictionaries
   * @return a list of detected names
   */
  public List<Name> getNames(String[] tokens) {
    
    List<Span> origSpans = nercToSpans(tokens);
    Span[] neSpans = NameFinderME.dropOverlappingSpans(origSpans.toArray(new Span[origSpans.size()]));
    List<Name> names = getNamesFromSpans(neSpans,tokens);
    return names;
  }
  
  /**
   * Detects Named Entities in a {@link Gazetteer} by type 
   * 
   * @param tokens
   * @param dictionaries
   * @return spans of the Named Entities all 
   */
   public List<Span> nercToSpans(String[] tokens) {
     List<Span> neSpans = new ArrayList<Span>();
     for (String neDict : gazetteer.gazetteerList) {
       List<Integer> neIds = StringUtils.exactTokenFinder(neDict,tokens);
       if (!neIds.isEmpty()) {
         Span neSpan = new Span(neIds.get(0), neIds.get(1), type);
         neSpans.add(neSpan); 
       }
     }
     return neSpans;
   }
  
  /**
   * Creates a list of {@link Name} objects from spans and tokens
   * 
   * @param neSpans
   * @param tokens
   * @return a list of name objects
   */
  public List<Name> getNamesFromSpans(Span[] neSpans, String[] tokens) {
    List<Name> names = new ArrayList<Name>();
    for (Span neSpan : neSpans) { 
      String nameString = StringUtils.getStringFromSpan(neSpan,tokens);
      String neType = neSpan.getType();
      Name name = nameFactory.createName(nameString, neType, neSpan);
      names.add(name);
    }
    return names;
  }
 
  /**
   * Concatenates two span lists adding the spans of the second parameter
   * to the list in first parameter
   * 
   * @param allSpans
   * @param neSpans
   */
  public void concatenateSpans(List<Span> allSpans, List<Span> neSpans) {
    for (Span span : neSpans) {
      allSpans.add(span);
    }
  }
  
  /**
   * Concatenates two span lists adding the spans to a new list
   * if the span in the second parameter does not 
   * exist in the first
   * 
   * @param preList
   * @param postList
   * @return a list containing the spans of the two lists if they are not duplicate
   */
  public List<Span> concatenateNoOverlappingSpans(List<Span> preList, List<Span> postList) {
    List<Span> allSpans = new ArrayList<Span>();
    for (Span span1 : preList) {
      if (DEBUG) {
        System.err.println("probabilistic spans" + preList.size());
        System.err.println("checking " + span1.toString());
      }
      for (Span span2 : postList) {
        if (!span1.contains(span2)) {
          allSpans.add(span1);
          if (DEBUG) {
            System.err.println("prob Span added! " + span1.toString());
          }
        }
        else if (span2.contains(span1)) {
          allSpans.add(span2);
          if (DEBUG) {
            System.err.println("dict span added! " + span2.toString());
          }
        }
        else {
          allSpans.add(span2);
        }
      }
    }
    return allSpans;
  }
  
  public void clearAdaptiveData() {
    // nothing to clear
  }

}