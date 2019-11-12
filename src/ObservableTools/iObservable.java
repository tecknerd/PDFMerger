package ObservableTools;

public interface iObservable {
	public void register(iObserver o);
	
	public void unregister (iObserver o);
	
	public void notifyObservers();
}
