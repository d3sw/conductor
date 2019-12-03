package com.netflix.conductor.aurora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.netflix.conductor.aurora.sql.ResultSetHandler;
import com.netflix.conductor.common.run.ErrorLookup;
import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.dao.ErrorLookupDAO;
import com.netflix.conductor.dao.ExecutionDAO;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuroraErrorLookupDAO extends AuroraBaseDAO implements ErrorLookupDAO {

	private ExecutionDAO executionDAO;

	@Inject
	public AuroraErrorLookupDAO(DataSource dataSource, ObjectMapper mapper, ExecutionDAO executionDAO, Configuration config) {
		super(dataSource, mapper);
		this.executionDAO = executionDAO;
	}

	@Override
	public List<ErrorLookup> getErrors() {

		return getWithTransaction( tx->{
            ErrorLookupHandler handler = new ErrorLookupHandler();

			String SQL = "SELECT * FROM meta_error_registry ORDER BY id";
			return query( tx, SQL, q->q.executeAndFetch(handler));
		});
	}

	@Override
	public List<ErrorLookup> getErrorMatching(String errorString) {
		return getWithTransaction( tx->{
            ErrorLookupHandler handler = new ErrorLookupHandler();

			StringBuilder SQL = new StringBuilder("select * from ( ");
			SQL.append("select substring(?, lookup) as matched_txt, * ");
			SQL.append("from meta_error_registry ");
			SQL.append(") as match_results ");
			SQL.append("where matched_txt is not null ");
			SQL.append("order by length(matched_txt) desc ");

			return query( tx, SQL.toString(), q-> q.addParameter(errorString).executeAndFetch(handler));

		});
	}

	@Override
	public boolean addError(ErrorLookup errorLookup) {
		validate(errorLookup);
		return insertOrUpdateErrorLookup(errorLookup);
	}

	@Override
	public boolean updateError(ErrorLookup errorLookup) {
		validate(errorLookup);
		return insertOrUpdateErrorLookup(errorLookup);
	}

	private boolean insertOrUpdateErrorLookup(ErrorLookup errorLookup) {

		String INSERT_SQL = "INSERT INTO meta_error_registry (error_code, lookup, workflow_name, general_message, root_cause, resolution) values (?,?,?,?,?,?)";
		String UPDATE_SQL = "UPDATE meta_error_registry SET error_code = ?, lookup = ?, workflow_name = ?, general_message = ?, root_cause = ?, resolution = ? WHERE id = ?";

		return getWithTransaction( tx->{
			int result = query( tx, INSERT_SQL, q->q.addParameter(errorLookup.getErrorCode())
					.addParameter(errorLookup.getLookup())
					.addParameter(errorLookup.getWorkflowName())
					.addParameter(errorLookup.getGeneralMessage())
					.addParameter(errorLookup.getRootCause())
					.addParameter(errorLookup.getResolution())
					.executeUpdate());

			if ( result > 0) {
				return true;
			}else{
				return query( tx, UPDATE_SQL, q->q.addParameter(errorLookup.getErrorCode())
						.addParameter(errorLookup.getLookup())
						.addParameter(errorLookup.getWorkflowName())
						.addParameter(errorLookup.getGeneralMessage())
						.addParameter(errorLookup.getRootCause())
						.addParameter(errorLookup.getResolution())
						.addParameter(errorLookup.getId())
						.executeUpdate() > 0);
			}
		});
	}

	class ErrorLookupHandler implements ResultSetHandler<ArrayList<ErrorLookup>> {

        public ArrayList<ErrorLookup> apply(ResultSet rs) throws SQLException {
            ArrayList<ErrorLookup> errorLookups = new ArrayList<>();

            while( rs.next()){
                ErrorLookup errorLookup = new ErrorLookup();
                errorLookup.setId( rs.getInt("id"));
                errorLookup.setErrorCode( rs.getString("error_code"));
                errorLookup.setLookup(rs.getString("lookup"));
                errorLookup.setWorkflowName(rs.getString("workflow_name"));
                errorLookup.setGeneralMessage(rs.getString("general_message"));
                errorLookup.setRootCause( rs.getString("root_cause"));
                errorLookup.setResolution( rs.getString("resolution"));
                errorLookups.add(errorLookup);
            }
            return errorLookups;
        }
    }


    private void validate(ErrorLookup errorLookup){
		Preconditions.checkNotNull(errorLookup, "ErrorLookup cannot be null");
	}
}
