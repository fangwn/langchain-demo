package com.fang.langchaindemo.tools;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

/**
 *
 * @author fangwennan
 * @date 2026/3/27 13:59
 */
@Component("ragProvider")
public class RagProvider {

    @Autowired
    @Qualifier("ollamaEmbeddingModel")
    OllamaEmbeddingModel ollamaEmbeddingModel;

    @Resource
    @Qualifier("tongYiEmbeddingModel")
    private OpenAiEmbeddingModel tongYiEmbeddingModel;

    public ContentRetriever loadHouseRulesRetriever() {
        Document doc = loadDocument(toPath("data/house_rules.txt"));
        InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(200, 10))
                .embeddingModel(tongYiEmbeddingModel)
                .embeddingStore(store)
                .build();

        ingestor.ingest(List.of(doc));

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(tongYiEmbeddingModel)
                .maxResults(2)
                .minScore(0.8)
                .build();
    }

    public static Path toPath(String relativePath) {
        try {
            URL fileUrl = Utils.class.getClassLoader().getResource(relativePath);
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
