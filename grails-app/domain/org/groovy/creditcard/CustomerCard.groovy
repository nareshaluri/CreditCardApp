package org.groovy.creditcard

class CustomerCard {
	int id
	int customer_id
	long cardNumber
	int creditLimit
	int isActive
	int approverID
	
	static mapping = {
		table 'customer_card'
		version false
		id generator:'increment', column:'id'
		cardNumber column:'cardNumber'
		creditLimit column:'creditLimit'
		isActive column:'isActive'
		approverID column:'approverID'
	}
	
    static constraints = {
    }
}
