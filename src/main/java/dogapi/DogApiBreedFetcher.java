package dogapi;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * BreedFetcher implementation that relies on the dog.ceo API.
 * Note that all failures get reported as BreedNotFoundException
 * exceptions to align with the requirements of the BreedFetcher interface.
 */
public class DogApiBreedFetcher implements BreedFetcher {
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Fetch the list of sub breeds for the given breed from the dog.ceo API.
     *
     * @param breed the breed to fetch sub breeds for
     * @return list of sub breeds for the given breed
     * @throws BreedNotFoundException if the breed does not exist (or if the API call fails for any reason)
     */
    @Override
    public List<String> getSubBreeds(String breed) throws BreedFetcher.BreedNotFoundException {
        if (breed == null || breed.isBlank()) {
            throw new BreedNotFoundException("Breed must be non-empty");
        }

        final String url = "https://dog.ceo/api/breed/"
                + breed.trim().toLowerCase(Locale.ROOT) + "/list";

        final Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("User-Agent", "DogBreedFetcher/1.0 (+https://example.com)")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) {
                throw new BreedNotFoundException("Empty response body for: " + breed);
            }

            final String body = response.body().string();

            // If HTTP not successful, dog.ceo often returns a JSON error; try to surface it.
            if (!response.isSuccessful()) {
                try {
                    JSONObject err = new JSONObject(body);
                    String msg = err.optString("message", "HTTP " + response.code());
                    throw new BreedNotFoundException(msg);
                } catch (org.json.JSONException ignored) {
                    throw new BreedNotFoundException("HTTP " + response.code() + " for: " + breed);
                }
            }

            final JSONObject json = new JSONObject(body);
            final String status = json.optString("status", "");
            if (!"success".equalsIgnoreCase(status)) {
                // dog.ceo uses { status:"error", message:"..." }
                String msg = json.optString("message", "Breed not found: " + breed);
                throw new BreedNotFoundException(msg);
            }

            final JSONArray arr = json.getJSONArray("message");
            final List<String> subBreeds = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                subBreeds.add(arr.getString(i));
            }
            subBreeds.sort(String.CASE_INSENSITIVE_ORDER);
            return subBreeds;

        } catch (IOException e) {
            // Network/SSL/timeouts
            throw new BreedNotFoundException("Network error fetching sub-breeds for: " + breed);
        } catch (org.json.JSONException e) {
            // Non-JSON body (e.g., 403 HTML) or unexpected schema
            throw new BreedNotFoundException("Unexpected response parsing for: " + breed);
        }
    }
}
