package org.cs4j.core;

/**
 * Created by user on 23/02/2017.
 */
public interface AnytimeSearchAlgorithm extends SearchAlgorithm {
    /**
     * Performs a search beginning at the specified state.
     *
     * @param domain The domain to apply the search on
     * @return search results
     */
    SearchResult search(SearchDomain domain);

    /**
     * Continues the search to find a better solution
     * @return A better solution, if such exists
     */
    SearchResult continueSearch();

    /**
     * Returns a SearchResults object that contains all the search results so
     */
    SearchResult getTotalSearchResults();
}
