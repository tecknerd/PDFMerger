package ObservableTools;

import java.util.Objects;
import java.util.function.Function;

import javax.swing.JButton;

public class TableRowObserverButton extends JButton implements iObserver {

	private Function<Integer, Boolean> fn;
	
	public TableRowObserverButton() {
		super();
		this.fn = null;
	}
	
	public TableRowObserverButton (Function<Integer, Boolean> fn) {
		super();
		this.fn = fn;
	}
	
	public TableRowObserverButton (String text, Function<Integer, Boolean> fn) {
		super(text);
		this.fn = fn;
	}
	
	public void setFunction(Function<Integer, Boolean> fn) {
		this.fn = fn;
	}
	
	@Override
	public void update(int size) {
		if(Objects.nonNull(fn)) {
			this.setEnabled(fn.apply(size));
		} else {
			throw new NullPointerException("Function is not set");
		}
	}
}
