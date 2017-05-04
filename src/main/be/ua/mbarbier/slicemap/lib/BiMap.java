/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.be.ua.mbarbier.slicemap.lib;

import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author mbarbier
 */
public class BiMap<K,V> {

    HashMap<K,V> map = new HashMap<>();
    HashMap<V,K> inversedMap = new HashMap<>();

    public void put(K k, V v) {
        map.put(k, v);
        inversedMap.put(v, k);
    }

	public void putAll( HashMap<K,V> inMap ) {

		for ( K k: inMap.keySet() ) {
			V v = inMap.get(k);
			map.put( k, v );
			inversedMap.put( v, k );
		}
    }

	public Set< K > getKeys() {
		return map.keySet();
	}

	public Set< V > getValues() {
		return inversedMap.keySet();
	}
	
	public BiMap<K,V> remove( K key ) {

		this.inversedMap.remove( this.map.get(key) );
		this.map.remove(key);

		return this;
	}

	public BiMap<K,V> copy() {

		BiMap<K,V> bimap = new BiMap<>();
		bimap.putAll( map );

		return bimap;
	}
	
    public V get(K k) {
        return map.get(k);
    }

    public K getKey(V v) {
        return inversedMap.get(v);
    }

}
