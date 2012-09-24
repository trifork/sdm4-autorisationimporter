/**
 * The MIT License
 *
 * Original work sponsored and donated by National Board of e-Health (NSI), Denmark
 * (http://www.nsi.dk)
 *
 * Copyright (C) 2011 National Board of e-Health (NSI), Denmark (http://www.nsi.dk)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dk.nsi.sdm4.autorisation.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import dk.nsi.sdm4.core.domain.TemporalEntity;


@Entity
public class Autorisation implements TemporalEntity
{
	private String nummer;
	private String cpr;
	private String efternavn;
	private String fornavn;
	private String educationCode;
	private Date validFrom;
	private Date validTo;


	public Autorisation()
	{}


	@Id
	@Column
	public String getAutorisationsnummer()
	{
		return nummer;
	}


	public void setAutorisationnummer(String nummer)
	{
		this.nummer = nummer;
	}


	@Column
	public String getCpr()
	{
		return cpr;
	}


	public void setCpr(String cpr)
	{
		this.cpr = cpr;
	}


	@Column
	public String getEfternavn()
	{
		return efternavn;
	}


	public void setEfternavn(String value)
	{
		efternavn = value;
	}


	@Column
	public String getFornavn()
	{
		return fornavn;
	}


	public void setFornavn(String value)
	{
		fornavn = value;
	}


	@Column
	public String getUddannelsesKode()
	{
		return educationCode;
	}


	public void setUddannelsesKode(String value)
	{
		educationCode = value;
	}


	@Override
	public Date getValidFrom()
	{
		return validFrom;
	}


	public void setValidFrom(Date value)
	{
		validFrom = value;
	}


	@Override
	public Date getValidTo()
	{
		return validTo;
	}


	public void setValidTo(Date value)
	{
		validTo = value;
	}
}
