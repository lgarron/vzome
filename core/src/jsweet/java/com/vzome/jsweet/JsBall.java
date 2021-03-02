package com.vzome.jsweet;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.construction.Construction;
import com.vzome.core.construction.FreePoint;
import com.vzome.core.model.Connector;

import def.js.Object;

public class JsBall extends JsManifestation implements Connector
{
    public JsBall( AlgebraicField field, Object adapter, int[][][] coords )
    {
        super( field, adapter, coords );
    }

    @Override
    public AlgebraicVector getLocation()
    {
        return ((JsAlgebraicField) this .field) .createVectorFromTDs( this .vectors[ 0 ] );
    }

    @Override
    public Construction toConstruction()
    {
        return new FreePoint( getLocation() );
    }

    @Override
    public int compareTo( Connector other )
    {
        if ( this == other ) {
            return 0;
        }
        if ( other .equals( this ) ) { // intentionally throws a NullPointerException if other is null
            return 0;
        }
        return this .getLocation() .compareTo( other .getLocation() );
    }
}
