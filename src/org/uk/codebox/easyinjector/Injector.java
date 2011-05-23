package org.uk.codebox.easyinjector;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Injector<T> {
	private Class<T> clazz;
	public Injector(Class<T> clazz){
		this.clazz = clazz;
	}
	
	private List<Hint> hints = new ArrayList<Injector.Hint>();
	public T build(){
		return build(clazz, hints.toArray(new Hint[0]));
	}
	public Injector<T> using(Class<?> clazz, Object... args){
		hints.add(new Hint(clazz, args));
		return this;
	}
	
	private <S> S build(Class<S> clazz, Hint... hints) {
		final Constructor<S>[] ctors = buildConstructorGraph(clazz);
		if (ctors.length > 0){
			S result = buildUsingHints(ctors, hints);
			if (result == null){
				Constructor<S> ctor = ctors[0];
				List<Object> args = new ArrayList<Object>();
				for (Class<?>  paramClass : ctor.getParameterTypes()) {
					args.add(build(paramClass, hints));
				}
				
				return makeNewInstance(ctor, args.toArray());

			} else {
				return result;
			}
			
		} else {
			throw new IllegalArgumentException(clazz.getName());	
		}
	}
	
	private  <S> S buildUsingHints(Constructor<S>[] ctors, Hint... hints) {
		for (Constructor<S> ctor : ctors) {
			outer: for (Hint hint : hints) {
				if (hint.clazz == ctor.getDeclaringClass()){
					if (ctor.getParameterTypes().length == hint.args.length){
						for(int i=0, l=hint.args.length; i<l; i++){
							Class<?> ctorArgClass = ctor.getParameterTypes()[i];
							Class<?> hintArgClass = hint.args[i].getClass();
							if (!ctorArgClass.isAssignableFrom(hintArgClass)){
								break outer;
							}
						}
						return makeNewInstance(ctor, hint.args);
					}
				}
			}
		}
		return null;
	}
	
	private <S> S makeNewInstance(Constructor<S> ctor, Object... args){
		try{
			return ctor.newInstance(args);
		} catch (Exception ex){
			throw new IllegalArgumentException(ex);
		}
	}
	
	private Map<Class<?>, Constructor<?>[]> typeBuildabilityMap = new HashMap<Class<?>, Constructor<?>[]>();
	
	private <S> Constructor<S>[] buildConstructorGraph(Class<S> clazz){
		if (!typeBuildabilityMap.containsKey(clazz)){
			typeBuildabilityMap.put(clazz, new Constructor<?>[0]);
			List<Constructor<?>> ctors = new ArrayList<Constructor<?>>();
			for (Constructor<?> constructor : clazz.getConstructors()) {
				boolean constructorOk = true;
				for(Class<?> paramClass : constructor.getParameterTypes()){
					if (buildConstructorGraph(paramClass).length == 0) {
						constructorOk = false;
						break;
					}
				}
				if (constructorOk){
					ctors.add(constructor);
				}
			}
			typeBuildabilityMap.put(clazz, ctors.toArray(new Constructor<?>[0]));
		}
		return (Constructor<S>[]) typeBuildabilityMap.get(clazz);
	}
	
	private static class Hint{
		public Hint(Class<?> clazz, Object... args) {
			this.clazz = clazz;
			this.args = args;
		}
		public Class<?> clazz;
		public Object[] args;
	}
	
}
