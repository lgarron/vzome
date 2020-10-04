
package com.vzome.core.algebra;

import com.vzome.core.math.RealVector;

public interface AlgebraicField
{
    public interface Registry
    {
        AlgebraicField getField( String name );
    }
    
    void defineMultiplier( StringBuffer instances, int w );

    int getOrder();

    int getNumIrrationals();

    String getIrrational( int i, int format );

    String getIrrational( int which );
    
    AlgebraicVector nearestAlgebraicVector( RealVector target );

    String getName();
    
    /**
     * Generates an AlgebraicNumber with integer terms (having only unit denominators). 
     * Use {@code createAlgebraicNumber( int[] numerators, int denominator )} 
     * if denominators other than one are required.
     * @param terms
     * @return
     */
    AlgebraicNumber createAlgebraicNumber( int[] terms );

    /**
     * Generates an AlgebraicNumber from a "trailing divisor" int array representation.
     * @param trailingDivisorForm numerators trailed by a common denominator for all numerators
     * @return
     */
    AlgebraicNumber createAlgebraicNumberFromTD( int[] trailingDivisorForm );

    /**
     * Generates an AlgebraicNumber with the specified numerators,
     * all having a common denominator as specified.
     * @param numerators
     * @param denominator is a common denominator for all numerators
     * @return
     */
    AlgebraicNumber createAlgebraicNumber( int[] numerators, int denominator );

    AlgebraicNumber createAlgebraicNumber( int ones, int irrat, int denominator, int scalePower );

    /**
     * The golden ratio (and thus icosahedral symmetry and related tools) 
     * can be generated by some fields even though it's not one of their irrational coefficients. 
     * For example, SqrtField(5) and PolygonField(10) can both generate the golden ratio 
     * so they can support icosa symmetry and related tools.
     * In some such cases, the resulting AlgebraicNumber 
     * may have multiple terms and/or factors other than one. 
     * 
     * @return An AlgebraicNumber which evaluates to the golden ratio, or null if not possible in this field.
     */
    AlgebraicNumber getGoldenRatio();

    AlgebraicNumber createPower( int power );

    AlgebraicNumber createPower( int power, int irr );

    /**
     * @param wholeNumber becomes the numerator with 1 as the denominator
     * @return AlgebraicNumber
     */
    AlgebraicNumber createRational( long wholeNumber );

    /**
     * @param numerator
     * @param denominator
     * @return AlgebraicNumber
     */
    AlgebraicNumber createRational( long numerator, long denominator );
        
    /**
     * @return The AlgebraicNumber to be use for the Chord Ratio construction in the given field.
     * This method can be used to generalize an AffinePolygon tool and a PolygonalAntiprismSymmetry.
     * This base class returns one, which is the scalar for an affine square and works in any field. 
     * Derived classes should override this method if they can be used to generate any other affine polygon.
     */
    AlgebraicNumber getAffineScalar();

    /**
     * @param n specifies the ordinal of the term in the AlgebraicNumber which will be set to one.
     * When {@code n == 0}, the result is the same as {@code createRational(1)}.
     * When {@code n == 1}, the result is the same as {@code createPower(1)}.
     * When {@code n < 0}, the result will be {@code zero()}.
     * When {@code n >= getOrder()}, an IndexOutOfBoundsException will be thrown.
     * @return an AlgebraicNumber with the factor specified by {@code n} set to one.
     */
    AlgebraicNumber getUnitTerm(int n);

    /**
     * Drop one coordinate from the 4D vector. If wFirst (the usual), then drop
     * the first coordinate, taking the "imaginary part" of the vector. If
     * !wFirst (for old VEF import, etc.), drop the last coordinate.
     *
     * @param source
     * @param wFirst
     * @return
     */
    AlgebraicVector projectTo3d( AlgebraicVector source, boolean wFirst );

    AlgebraicVector origin( int dims );

    AlgebraicVector basisVector( int dims, int axis );

    // ======================================================================================
    // number operations
    // ======================================================================================

    static int DEFAULT_FORMAT = 0; // 4 + 3φ

    static int EXPRESSION_FORMAT = 1; // 4 +3*phi

    static int ZOMIC_FORMAT = 2; // 4 3

    static int VEF_FORMAT = 3; // (3,4)

    AlgebraicNumber zero();

    AlgebraicNumber one();

    /**
     *
     * @param nums is an array of integer arrays: One array of coordinate terms per dimension.
     * Initially, this is designed to simplify migration of order 2 golden directions
     * to new fields of higher order having golden subfields as their first two factors.
     {@code
        field.createVector( new int[]  {  0,1,2,3,   4,5,6,7,   8,9,0,1  } );   // older code like this...
        field.createVector( new int[][]{ {0,1,2,3}, {4,5,6,7}, {8,9,0,1} } );   // should be replaced by this...
        field.createVector( new int[][]{ {0,1,2,3}, {4,5,6,7}, {8,9    } } );   // ... or even this.
     }
     * The older code shown in the first example requires an order 2 field.
     * The second example will work with any field of order 2 or greater.
     * This new overload has the advantage that the internal arrays representing the individual dimensions are more clearly delineated and controlled.
     * As shown in the third example, the internal arrays need not be all the same length. Trailing zero terms can be omitted as shown.
     * Inner arrays require an even number of elements since they represent a sequence of numerator/denominator pairs.
     * 
     * createVector is currently limited to int valued vectors, not long, and definitely not BigInteger
     * In most cases, this is adequate, but in the case where it's called by XmlSaveFormat.parseAlgebraicObject(), 
     * it seems possible that a value larger than Integer.MAX_VALUE could be saved to the XML which could not subsequently be parsed.
     * TODO: Consider refactoring createVector to use long[][] instead of int[][] if this becomes an issue. 
     * 
     * @return an AlgebraicVector
     */
    AlgebraicVector createVector( int[][] nums );
    
    /**
     * 
     * @param nums nums is an array of integer arrays: One array of coordinate terms per dimension.
     * Each inner array is in "trailing divisor" form, to represent a rational AlgebraicNumber.
     * If the order of the field is N, each inner array will be of length N+1, with the last
     * element being the divisor.
     * @return
     */
    AlgebraicVector createVectorFromTDs( int[][] nums );
    
    /**
     * Generates an AlgebraicVector with all AlgebraicNumber terms being integers (having unit denominators).
     * Contrast this with {@code createVector(int[][] nums)} which requires all denominators to be specified.
     * @param nums is a 2 dimensional integer array. The length of nums becomes the number of dimensions in the resulting AlgebraicVector.
     * For example, {@code (new PentagonField()).createIntegerVector( new int[][]{ {0,-1}, {2,3}, {4,5} } ); } 
     * generates the 3 dimensional vector (-φ, 2 +3φ, 4 +5φ) having all integer terms. 
     * @return an AlgebraicVector
     */
    AlgebraicVector createIntegerVector( int[][] nums );
    
    /**
     * Generates an AlgebraicVector with all AlgebraicNumber terms in "trailing divisor" int array form.
     * @param nums is a 2 dimensional integer array. The length of nums becomes the number of dimensions in the resulting AlgebraicVector.
     * For example, {@code (new PentagonField()).createIntegerVectorFromTDs( new int[][]{ {0,-1,1}, {2,3,2}, {4,5,2} } ); } 
     * generates the 3 dimensional vector (-φ, 1 +3φ/2, 2 +5φ/2). 
     * @return an AlgebraicVector
     */
    AlgebraicVector createIntegerVectorFromTDs( int[][] nums );

    /**
     * Create a 3x3 square matrix from integer data.
     * TODO: Generalize this method to create a matrix with dimensions matching the dimensions of the data array
     * Sample input data for an order-4 field:
     *   {{{7,5,0,1,-4,5,0,1},{-2,5,0,1,4,5,0,1},{0,1,-8,5,0,1,6,5}},
     *    {{-2,5,0,1,4,5,0,1},{7,5,0,1,-4,5,0,1},{0,1,8,5,0,1,-6,5}},
     *    {{0,1,-8,5,0,1,6,5},{0,1,8,5,0,1,-6,5},{-9,5,0,1,8,5,0,1}}}
     * @param field
     * @param data integer coordinates, in row-major order, complete with denominators.
     * @return
     */
    AlgebraicMatrix createMatrix( int[][][] data );

    AlgebraicNumber parseLegacyNumber( String val );

    AlgebraicNumber parseNumber( String nums );

    AlgebraicVector parseVector( String nums );

    AlgebraicMatrix identityMatrix( int dims );

    /**
     * @return the number of independent multipliers in this field.
     * These are the primitive elements of the field.
     * The value should be less than or equal to getNumIrrationals.
     * It will be less whenever the irrationals are dependent.
     * For example, in the field for sqrt(phi), there is only one
     * multiplier, since the other irrational is just the square of that one.
     */
    int getNumMultipliers();

    AlgebraicNumber parseVefNumber( String string, boolean isRational );
    
    boolean scale4dRoots();
    
    boolean doubleFrameVectors();
}
