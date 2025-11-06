package com.thiagotoazza.data.source.company

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.thiagotoazza.data.models.company.Company
import com.thiagotoazza.utils.Constants
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

class MongoCompanyDataSource(database: MongoDatabase) : CompanyDataSource {

    private val companiesCollection = database.getCollection<Company>(Constants.KEY_COMPANIES_COLLECTION)

    override suspend fun getCompanies(): List<Company> {
        return companiesCollection.find().toList()
    }

    override suspend fun getCompanyById(companyId: String?): Company? {
        val query = Document("_id", ObjectId(companyId))
        return companiesCollection.find<Company>(query).firstOrNull()
    }

    override suspend fun insertCompany(company: Company): Boolean {
        return companiesCollection.insertOne(company).wasAcknowledged()
    }

    override suspend fun updateCompany(company: Company): Boolean {
        val query = Document("_id", company.id)
        return companiesCollection.replaceOne(query, company).wasAcknowledged()
    }

    override suspend fun deleteCompany(companyId: String?): Boolean {
        val query = Document("_id", ObjectId(companyId))
        return companiesCollection.deleteOne(query).wasAcknowledged()
    }

}