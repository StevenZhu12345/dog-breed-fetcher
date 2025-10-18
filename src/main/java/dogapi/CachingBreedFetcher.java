package dogapi;

import java.util.*;

/**
 * This BreedFetcher caches fetch request results to improve performance and
 * lessen the load on the underlying data source. An implementation of BreedFetcher
 * must be provided. The number of calls to the underlying fetcher are recorded.
 *
 * If a call to getSubBreeds produces a BreedNotFoundException, then it is NOT cached
 * in this implementation. The provided tests check for this behaviour.
 *
 * The cache maps the name of a breed to its list of sub breed names.
 */
public class CachingBreedFetcher implements BreedFetcher {
    // TODO Task 2: Complete this class
    private final BreedFetcher delegate;
    private final Map<String, List<String>> cache = new HashMap<>();
    private int callsMade = 0;

    public CachingBreedFetcher(BreedFetcher fetcher) {
        if (fetcher == null) throw new IllegalArgumentException("fetcher cannot be null");
        this.delegate = fetcher;
    }

    @Override
    public List<String> getSubBreeds(String breed) throws BreedFetcher.BreedNotFoundException {
        if (breed == null || breed.isBlank()) {
            throw new BreedNotFoundException("Breed must be non-empty");
        }

        final String key = breed.trim().toLowerCase(Locale.ROOT);
        List<String> cached = cache.get(key);
        if (cached != null) return new ArrayList<>(cached);

        // miss → call delegate and cache success
        callsMade++;
        List<String> result = delegate.getSubBreeds(breed); // may throw (propagate)
        cache.put(key, new ArrayList<>(result));
        return new ArrayList<>(result);
    }

    public int getCallsMade() {
        return callsMade;
    }
}