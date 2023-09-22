package btdex.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RemoteSignumResources {
	static RemoteSignumResources instance;

	private final String baseURL = Constants.REMOTE_NODE_RESOURCES_URL;
	private final OkHttpClient httpClient = new OkHttpClient();

	private Set<String> testnetNodes = new HashSet<>();
	private Set<String> mainnetNodes = new HashSet<>();
	private Set<String> testnetPools = new HashSet<>();
	private Set<String> mainnetPools = new HashSet<>();

	private Logger logger;

	public static RemoteSignumResources getInstance() {
		if (instance == null)
			instance = new RemoteSignumResources();
		return instance;
	}

	private RemoteSignumResources() {
		this.logger = LogManager.getLogger();
	}

	public RemoteSignumResources loadNodeLists() throws IOException {
		try {
			logger.debug("Fetching remote node list from {}...", this.baseURL);
			Request request = new Request
				.Builder()
				.url(this.baseURL + "/nodes.json")
				.addHeader("Accept", "application/json")
				.build();
			Response response = this.httpClient.newCall(request).execute();

			assert response.code() == 200;
			assert response.body() != null;

			String body = response.body().string();
			JsonObject json = JsonParser.parseString(body).getAsJsonObject();
			json.get("mainnet").getAsJsonArray().forEach(node -> {
				this.mainnetNodes.add(node.getAsJsonObject().get("url").getAsString());
			});
			json.get("testnet").getAsJsonArray().forEach(node -> {
				this.testnetNodes.add(node.getAsJsonObject().get("url").getAsString());
			});
		}catch(Exception e){
			logger.error(e);
			logger.debug("Loading Remote node list failed. Using constant fallbacks");
			this.mainnetNodes.addAll(Arrays.asList(Constants.NODE_LIST));
			this.testnetNodes.addAll(Arrays.asList(Constants.NODE_LIST_TESTNET));
		}
		return this;
	}
	public RemoteSignumResources loadPoolLists() throws IOException {
		try {
			logger.debug("Fetching remote pool list from {}...", this.baseURL);
			Request request = new Request
				.Builder()
				.url(this.baseURL + "/pools.json")
				.addHeader("Accept", "application/json")
				.build();
			Response response = this.httpClient.newCall(request).execute();

			assert response.code() == 200;
			assert response.body() != null;

			String body = response.body().string();
			JsonObject json = JsonParser.parseString(body).getAsJsonObject();
			json.get("mainnet").getAsJsonArray().forEach(node -> {
				this.mainnetPools.add(node.getAsJsonObject().get("url").getAsString());
			});
			json.get("testnet").getAsJsonArray().forEach(node -> {
				this.testnetPools.add(node.getAsJsonObject().get("url").getAsString());
			});
		}catch(Exception e){
			logger.error(e);
			logger.debug("Loading Remote pool list failed. Using constant fallbacks");
			this.mainnetPools.addAll(Arrays.asList(Constants.POOL_LIST));
			this.testnetPools.addAll(Arrays.asList(Constants.POOL_LIST_TESTNET));
		}
		return this;
	}

	public Set<String> testNetNodes() {
		return this.testnetNodes;
	}
	public Set<String> mainNetNodes() {
		return this.mainnetNodes;
	}

	public Set<String> testNetPools() {
		return this.testnetPools;
	}
	public Set<String> mainNetPools() {
		return this.mainnetPools;
	}
}
