
package io.github.mightguy.spellcheck.symspell.common;

import java.util.Comparator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@Setter
public class SuggestionItem implements Comparator<SuggestionItem>, Comparable<SuggestionItem> {

  /**
   * The suggested correctly spelled word.
   */
  private String term;

  /**
   * Edit distance between searched for word and suggestion.
   */
  private double distance;

  /**
   * Frequency of suggestion in the dictionary (a measure of how common the word is).
   */
  private double count;


  private String metaphoneCode;

  private boolean sameMetaphoneCode;

  /**
   * COnstructor  for SuggestionItem
   * @param term
   * @param distance
   * @param count
   */
  public SuggestionItem(String term, double distance, double count) {
    this.term = term;
    this.distance = distance;
    this.count = count;
  }

  /**
   * final similarity
   */
  private double score;

  /**
   * Comparison to use in Sorting: Prefernce given to distance, and if distance is same then count
   */
  @Override
  public int compareTo(SuggestionItem other) {

    if (this.distance == 0)
      return -1;
    else if (other.distance == 0)
      return 1;

   // if (SpellHelper.isEqualDouble(this.distance, other.distance, 0.001f)) {

      if (SpellHelper.isEqualDouble(this.score, other.score, 0.0000000001f))
      {
        return Double.compare(other.count, this.count);
      }
      return Double.compare(other.score/Math.pow(100, other.distance), this.score/Math.pow(100, this.distance));

   // }
  //  return Double.compare(this.distance, other.distance);
  }

  @Override
  public int compare(SuggestionItem suggestItem, SuggestionItem suggestItem2) {
    return suggestItem.compareTo(suggestItem2);
  }

}
