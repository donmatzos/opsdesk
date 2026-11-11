package at.donmatzos.opsdesk.ai.search;

import java.util.List;

/** Finds the runbook sections that best match a query; best first, never null. */
public interface Retriever {

    List<RetrievedChunk> search(String query, int limit);
}
