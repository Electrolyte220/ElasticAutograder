import { downloadResults } from "../api/download_file";
import { useMemo } from "react";
import { AgGridProvider, AgGridReact } from "ag-grid-react";
import { AllCommunityModule } from "ag-grid-community";
import { themeQuartz } from "ag-grid-community";

const theme = themeQuartz
	.withParams({
        backgroundColor: "#1f2836",
        browserColorScheme: "dark",
        chromeBackgroundColor: {
            ref: "foregroundColor",
            mix: 0.07,
            onto: "backgroundColor"
        },
        foregroundColor: "#FFF",
        headerFontSize: 14
    });

export default function JobsTable({ jobs, gridRef }) {
  const colDefs = useMemo(() => [
    { field: "id", headerName: "ID", cellDataType: "number", width: 75, flex:0 },
    { field: "graderType", headerName: "Grader Type", valueFormatter: ({ value }) => value ?? "" },
    { field: "originalFilename", headerName: "Filename", valueFormatter: ({ value }) => value ?? "" },
    { field: "status", headerName: "Status", valueFormatter: ({ value }) => value ?? "", width: 125, flex:0 },
    { field: "createdAt", headerName: "Created At", valueFormatter: ({ value }) => value ? formatDate(value) : "", width: 240, flex:0 },
    { field: "score", headerName: "Score", cellDataType: "number", valueFormatter: ({ value }) => value ?? "", width: 85, flex:0 },
    { field: "tests", headerName: "Tests", valueGetter: ({ data }) => formatTests(data), width: 85, flex:0 },
    { headerName: "Download Results", cellRenderer: downloadResultsCellRenderer, sortable: false, filter: false, width: 160, flex:0  }
  ], []);

    return (
      <AgGridProvider modules={[AllCommunityModule]}>
        <div>
          <AgGridReact
            ref={gridRef}
            rowData={jobs}
            columnDefs={colDefs}
            defaultColDef={{filter:true, flex:1}}
            gridOptions={{
              theme: theme,
              pagination: true,
              paginationPageSize: 5,
              paginationPageSizeSelector: [5, 10, 25, 100],
              domLayout: 'autoHeight'
            }}
            getRowId={({ data }) => String(data.id)}
          />
        </div>
      </AgGridProvider>
    )
}

function formatDate(date) {
  return new Intl.DateTimeFormat("en-US", {
    year: "numeric", month: "long", day: "numeric",
    hour: "2-digit", minute: "numeric", second: "numeric"
  }).format(new Date(date));
}

function formatTests(data) {
  return data.testsPassed != null && data.testsTotal != null
    ? `${data.testsPassed} / ${data.testsTotal}`
    : "";
}

function downloadResultsCellRenderer({ data }) {
  const disabledStatus = data.status !== "SUCCEEDED" && data.status !== "FAILED";
  return (
    <button onClick={() => handleDownload(data.id)} disabled={disabledStatus}>
      Download Results
    </button>
  )
}

const handleDownload = async (id) => {
  try {
    const blob = await downloadResults(id);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "results.json";
    a.click();
    URL.revokeObjectURL(url);
  } catch(err) {
      alert("Could not download results file.")
      throw new Error("Could not download results file.\n" + err);
  }
}
