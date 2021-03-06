/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.commons.metadata.mqleditor.editor.gwt;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.commons.connection.IPentahoResultSet;
import org.pentaho.commons.metadata.mqleditor.MqlDomain;
import org.pentaho.commons.metadata.mqleditor.MqlQuery;
import org.pentaho.commons.metadata.mqleditor.editor.gwt.util.MQLEditorGwtService;
import org.pentaho.commons.metadata.mqleditor.editor.service.util.MQLEditorServiceDelegate;
import org.pentaho.metadata.query.model.util.QueryXmlHelper;
import org.pentaho.metadata.repository.FileBasedMetadataDomainRepository;
import org.pentaho.metadata.repository.IMetadataDomainRepository;
import org.pentaho.metadata.util.XmiParser;
import org.pentaho.platform.api.data.IDBDatasourceService;
import org.pentaho.platform.api.engine.IPentahoDefinableObjectFactory.Scope;
import org.pentaho.platform.api.engine.ISolutionEngine;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.services.connection.datasource.dbcp.JndiDatasourceService;
import org.pentaho.platform.engine.services.solution.SolutionEngine;
import org.pentaho.platform.plugin.action.pentahometadata.MetadataQueryComponent;
import org.pentaho.platform.plugin.services.connections.sql.SQLConnection;
import org.pentaho.pms.schema.SchemaMeta;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class MQLEditorDebugGwtServlet extends RemoteServiceServlet implements MQLEditorGwtService {

  private MQLEditorServiceDelegate delegate;
  private SchemaMeta meta;
  private MicroPlatform microPlatform;
  private static Log log = LogFactory.getLog( MQLEditorDebugGwtServlet.class );

  public MQLEditorDebugGwtServlet() {

    microPlatform = new MicroPlatform( "resources/solution1/" );
    microPlatform.define( ISolutionEngine.class, SolutionEngine.class );
    microPlatform.define( IMetadataDomainRepository.class, FileBasedMetadataDomainRepository.class, Scope.GLOBAL );
    microPlatform.define( "connection-SQL", SQLConnection.class );

    microPlatform.define( IDBDatasourceService.class, JndiDatasourceService.class, Scope.GLOBAL );
    // JNDI
    System.setProperty( "java.naming.factory.initial", "org.osjava.sj.SimpleContextFactory" ); //$NON-NLS-1$ //$NON-NLS-2$
    System.setProperty( "org.osjava.sj.root", "resources/solution1/simple-jndi" ); //$NON-NLS-1$ //$NON-NLS-2$
    System.setProperty( "org.osjava.sj.delimiter", "/" ); //$NON-NLS-1$ //$NON-NLS-2$

    microPlatform.init();
    new StandaloneSession();

    FileBasedMetadataDomainRepository repo =
        (FileBasedMetadataDomainRepository) PentahoSystem.get( IMetadataDomainRepository.class, null );
    //    repo.setDomainFolder("resources/solution1/system/metadata/domains"); //$NON-NLS-1$

    // Parse and add legacy CWM domain for testing purposes.
    XmiParser parser = new XmiParser();
    try {
      InputStream inStr =
          getClass().getResourceAsStream( "/org/pentaho/commons/metadata/mqleditor/sampleMql/metadata_steelwheels.xmi" ); //$NON-NLS-1$
      if ( inStr != null ) {
        org.pentaho.metadata.model.Domain d = parser.parseXmi( inStr );
        d.setId( "Steel-Wheels" ); //$NON-NLS-1$
        repo.storeDomain( d, false );
        repo.reloadDomains();

      } else {
        System.out.println( "Error loading XMI file" );
        // System.exit(1);
      }
    } catch ( Exception e ) {
      System.out.println( "error with XMI input" ); //$NON-NLS-1$
    }

    delegate = new MQLEditorServiceDelegate( repo );

  }

  public MqlDomain getDomainByName( String name ) {
    return delegate.getDomainByName( name );
  }

  public List<MqlDomain> refreshMetadataDomains() {
    return delegate.refreshMetadataDomains();
  }

  public List<MqlDomain> getMetadataDomains() {
    return delegate.getMetadataDomains();
  }

  public String saveQuery( MqlQuery model ) {
    return delegate.saveQuery( model );
  }

  public String serializeModel( MqlQuery query ) {
    return delegate.serializeModel( query );
  }

  public String[][] getPreviewData( MqlQuery query, int page, int limit ) {
    org.pentaho.metadata.query.model.Query mqlQuery = delegate.convertQueryModel( query );

    MetadataQueryComponent component = new MetadataQueryComponent();

    String mqlString = new QueryXmlHelper().toXML( mqlQuery );

    component.setQuery( mqlString );
    component.setLive( false );

    if ( component.execute() ) {
      IPentahoResultSet rs = component.getResultSet();
      String[][] results = new String[Math.min( rs.getRowCount(), limit )][rs.getColumnCount()];

      for ( int i = 0; i < Math.min( rs.getRowCount(), limit ); i++ ) {
        for ( int j = 0; j < rs.getColumnCount(); j++ ) {
          results[i][j] = "" + rs.getValueAt( ( ( page - 1 ) * limit ) + i, j ); //$NON-NLS-1$
        }
      }
      return results;
    } else {
      return null;
    }

  }

  public MqlQuery deserializeModel( String serializedQuery ) {
    return delegate.deserializeModel( serializedQuery );
  }

}
